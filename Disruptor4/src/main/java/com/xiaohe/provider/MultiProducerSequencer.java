package com.xiaohe.provider;

import com.xiaohe.common.Sequence;
import com.xiaohe.exception.InsufficientCapacityException;
import com.xiaohe.util.Util;
import com.xiaohe.util.wait.WaitStrategy;
import sun.misc.Unsafe;

import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-03 10:47
 */
public final class MultiProducerSequencer extends AbstractSequencer {
    private static final Unsafe UNSAFE = Util.getUnsafe();
    /**
     * 得到数组第一个位置的偏移量
     */
    private static final long BASE = UNSAFE.arrayBaseOffset(int[].class);
    /**
     * 数组每一个元素引用的大小
     */
    private static final long SCALE = UNSAFE.arrayIndexScale(int[].class);

    /**
     * 多消费者中消费最慢的那个进度
     */
    private final Sequence gatingSequenceCache = new Sequence(Sequence.INITIAL_VALUE);

    /**
     * 真正用来表示生产者进度的数组。消费者消费时参考的就是这个数组
     * 这个数组如果是连续的，消费者才能消费
     */
    private final int[] availableBuffer;

    private final int indexMask;

    private final int indexShift;
    public MultiProducerSequencer(int bufferSize, WaitStrategy waitStrategy) {
        super(bufferSize, waitStrategy);
        this.availableBuffer = new int[bufferSize];
        this.indexMask = bufferSize - 1;
        this.indexShift = Util.log2(bufferSize);
        // 在构造方法中，将数组中所有位置都设置为不可用
        for (int i = 0; i < availableBuffer.length; i++) {
            setAvailableBufferValue(i, -1);
        }
    }

    private void setAvailable(final long sequence) {
        int index = calculateIndex(sequence);
        int value = calculateAvailabilityFlag(sequence);
        setAvailableBufferValue(index, value);
    }
    /**
     * 将数组中 index 位置的值设置为 value。生产者发布数据之后可通过这个方法通知消费者
     * @param index
     * @param value
     */
    private void setAvailableBufferValue(int index, int value) {
        long bufferAddress = (index * SCALE) + BASE;
        UNSAFE.putOrderedInt(availableBuffer, bufferAddress, value);
    }

    /**
     * 计算这个值在 buffer 中的下标
     * @param sequence
     * @return
     */
    private int calculateIndex(final long sequence) {
        return ((int) sequence) & indexMask;
    }

    private int calculateAvailabilityFlag(final long sequence) {
        return (int) (sequence >>> indexShift);
    }

    /**
     * 判断是否有充足的容量分配需要的序号，这里只是判断，并不是要分配
     * @param requiredCapacity
     * @return
     */
    @Override
    public boolean hasAvailableCapacity(int requiredCapacity) {
        return hasAvailableCapacity(gatingSequences, requiredCapacity, cursor.get());
    }

    /**
     * 判断是否有充足的容量分配需要的序号，这里只是判断，并不是要分配
     * @param gatingSequences
     * @param requiredCapacity
     * @param cursorValue
     * @return
     */
    private boolean hasAvailableCapacity(Sequence[] gatingSequences, final int requiredCapacity, long cursorValue) {
        long wrapPoint = (cursorValue + requiredCapacity) - bufferSize;
        // 先不获取最新进度，因为如果判断了不行再用最新进度
        long cachedGatingSequence = gatingSequenceCache.get();
        if (wrapPoint > cachedGatingSequence || cachedGatingSequence > cursorValue) {
            long minSequence = Util.getMinimumSequence(gatingSequences, cursorValue);
            gatingSequenceCache.set(minSequence);
            if (wrapPoint > minSequence) {
                return false;
            }
        }
        return true;
    }

    /**
     * 得到环形数组的剩余容量
     * @return
     */
    @Override
    public long remainingCapacity() {
        // 最慢消费进度
        long consumed = Util.getMinimumSequence(gatingSequences, cursor.get());
        // 生产序号的分发进度
        long produced = cursor.get();
        return getBufferSize() - (produced - consumed);
    }

    /**
     * 申请一个可用序号
     * @return
     */
    @Override
    public long next() {
        return next(1);
    }

    @Override
    public long next(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be > 0");
        }
        long current;
        long next;
        do {
            current = cursor.get();
            next = current + n;
            long wrapPoint = next - bufferSize;
            long cachedGatingSequence = gatingSequenceCache.get();
            // 如果可能发生覆盖数据的情况，更新当前消费的进度
            if (wrapPoint > cachedGatingSequence || cachedGatingSequence > current) {
                // 拿到最新的消费进度，如果还可能发生覆盖数据的情况，让当前线程睡觉，睡觉完进入下一个循环重新判断
                long gatingSequence = Util.getMinimumSequence(gatingSequences, current);
                if (wrapPoint > gatingSequence) {
                    LockSupport.parkNanos(1);
                    continue;
                }
                // 更新一下进度
                gatingSequenceCache.set(gatingSequence);
            } else if (cursor.compareAndSet(current, next)) {
                // 不会发生覆盖进度的问题，并且使用CAS转换成功，那么这个序号算是申请成功了
                break;
            }
        } while(true);
        return next;
    }

    /**
     * 尝试申请1个可用序号
     * @return
     * @throws InsufficientCapacityException
     */
    @Override
    public long tryNext() throws InsufficientCapacityException {
        return tryNext(1);
    }

    @Override
    public long tryNext(int n) throws InsufficientCapacityException {
        if (n < 1) {
            throw new IllegalArgumentException("n must be > 0");
        }
        long current;
        long next;
        do {
            current = cursor.get();
            next = current + n;
            // 判断一下是否有这么多可用
            if (!hasAvailableCapacity(gatingSequences, n, current)) {
                throw InsufficientCapacityException.INSTANCE;
            }
        } while (!cursor.compareAndSet(current, next));
        return 0;
    }

    /**
     * 通知消费者，sequence可用了
     * @param sequence
     */
    @Override
    public void publish(long sequence) {
        setAvailable(sequence);
        waitStrategy.signalAllWhenBlocking();
    }

    /**
     * 通知消费者，从 lo 到 h1 的数据是可用的
     * @param lo
     * @param hi
     */
    @Override
    public void publish(long lo, long hi) {
        for (long l = lo; l <= hi; l++) {
            setAvailable(l);
        }
        // 将阻塞的全部唤醒
        waitStrategy.signalAllWhenBlocking();
    }

    /**
     * 将生产者的进度序号指定为 sequence
     * @param sequence
     */
    @Override
    public void claim(long sequence) {
        cursor.set(sequence);
    }

    /**
     * 判断该序号是否可用，或者说，这个序号分配出去了，但是它是否有数据呢
     * @param sequence
     * @return
     */
    @Override
    public boolean isAvailable(long sequence) {
        int index = calculateIndex(sequence);
        int flag = calculateAvailabilityFlag(sequence);
        long bufferAddress = (index * SCALE) + BASE;
        return UNSAFE.getIntVolatile(availableBuffer, bufferAddress) == flag;
    }

    @Override
    public long getHighestPublishedSequence(long lowerBound, long availableSequence) {
        for (long sequence = lowerBound; sequence <= availableSequence; sequence++) {
            if (!isAvailable(lowerBound)) {
                return sequence - 1;
            }
        }
        return availableSequence;
    }
}
