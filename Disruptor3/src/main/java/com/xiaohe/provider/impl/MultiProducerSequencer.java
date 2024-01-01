package com.xiaohe.provider.impl;

import com.xiaohe.Sequence;
import com.xiaohe.provider.AbstractSequencer;
import com.xiaohe.util.Util;
import com.xiaohe.wait.WaitStrategy;
import sun.misc.Unsafe;

import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description : 多生产者模式的序号生成器
 * @date : 2024-01-01 11:32
 */
public final class MultiProducerSequencer extends AbstractSequencer {

    private final Sequence gatingSequenceCache = new Sequence(Sequence.INITIAL_VALUE);

    private static final Unsafe UNSAFE = Util.getUnsafe();

    private static final long BASE = UNSAFE.arrayBaseOffset(int[].class);

    private static final long SCALE = UNSAFE.arrayIndexScale(int[].class);
    /**
     * 真正用来表明生产者进度的数值，消费者消费数据时，真正参考的是这个数组
     * 这个数组中的数据如果是连续的，消费者才能消费
     * 比如，消费者得到的可消费序号为 8，当前的消费进度为 3，那就意味着 3 - 8 都可以消费
     * 但是如果 3 - 8 之间的数据在这个数组中不连续，例如第六个位置没有数据，那么消费者就只能消费 3 - 6
     * availableBuffer[i] : 环形数组中第 i 个元素消费到第几轮了
     * i : 环形数组中第 i 个元素
     */
    private final int[] availableBuffer;
    /**
     * 用于计算上面那个数组下标的掩码
     */
    private final int indexMask;
    /**
     * 计算 availableBuffer 中可用标志圈数的辅助属性，数组长度为8，那么这个值就是3
     */
    private final int indexShift;

    public MultiProducerSequencer(int bufferSize, WaitStrategy waitStrategy) {
        super(bufferSize, waitStrategy);
        availableBuffer = new int[bufferSize];
        indexMask = bufferSize - 1;
        indexShift = Util.log2(bufferSize);
        // 将数组中所有位置都设置为不可消费
        initialiseAvailableBuffer();
    }

    private void initialiseAvailableBuffer() {
        for (int i = availableBuffer.length - 1; i != 0; i--) {
            setAvailableBufferValue(i, -1);
        }
        setAvailableBufferValue(0, -1);
    }
    // 把能消费的进度设置到availableBuffer数组中
    private void setAvailable(final long sequence){
        // 这里得到的就是生产者序号在availableBuffer数组中的下标索引以及对应的圈数
        // 假如 sequence = 6，也就是下一次可以消费6。环形数组为8
        // index = 6 & 7 = 6
        // flag = 6 >>> 3 = 0
        // 将数组下标为 6 的数据设置为0
        // 假如 sequence = 14, 也就是下一次可以消费14。环形数组大小为8
        // index = 14 & 7 = 7
        // flag = 14 (1110) >>> 3 = 1
        // 将数组下标为 7 的数据设置为 1
        int index = calculateIndex(sequence);
        int flag = calculateAvailabilityFlag(sequence);
        setAvailableBufferValue(index, flag);
    }

    //真正把能消费的进度设置到availableBuffer数组中的方法
    private void setAvailableBufferValue(int index, int flag){
        // 快速定位到指定下标在数组中的位置
        long bufferAddress = (index * SCALE) + BASE;
        //把对应的圈数写到对应的位置
        UNSAFE.putOrderedInt(availableBuffer, bufferAddress, flag);
    }
    //计算可用标志在availableBuffer数组中是第几圈的方法
    private int calculateAvailabilityFlag(final long sequence){
        //如果这里环形数组的长度是8，indexShift就是3，如果生产者序号是6，右移3，结果是0
        //其实换成二进制就一目了然了8的二进制是 前面省略...... 0000 1000，右移3位正好是1
        //如果生产者序号是9，说明是第一圈
        //如果生产者序号是17，右移3位，得到的就是2
        //17的二进制为 前面省略...... 0001 0001
        return (int) (sequence >>> indexShift);
    }
    //计算分配到的生产者序号在availableBuffer数组中的下标位置
    //这个availableBuffer数组和环形数组的长度是一样的
    private int calculateIndex(final long sequence){
        return ((int) sequence) & indexMask;
    }

    /**
     * 判断序号 sequence 是否可以消费
     * @param sequence
     * @return
     */
    public boolean isAvailable(long sequence) {
        int index = calculateIndex(sequence);
        int flag = calculateAvailabilityFlag(sequence);
        long bufferAddress = BASE + (index * SCALE);
        return UNSAFE.getIntVolatile(availableBuffer, bufferAddress) == flag;
    }

    /**
     * 计算从 lowerBound 从 availableSequence 之间哪些数据能够消费
     * @param lowerBound
     * @param availableSequence
     * @return
     */
    public long getHighestPublishedSequence(long lowerBound, long availableSequence) {
        for (long sequence = lowerBound; sequence <= availableSequence; sequence++) {
            if (!isAvailable(sequence)) {
                return sequence - 1;
            }
        }
        return availableSequence;
    }
    public long next(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be > 0");
        }
        // 当前已经分配到的生产序号
        long current;
        // 接下来要分配的生产序号
        long next;
        do {
            current = cursor.get();
            next = current + n;
            // 用于判断是否会覆盖未消费的数据
            long wrapPoint = next - bufferSize;
            // 得到消费者的进度
            long cacheGatingSequence = gatingSequenceCache.get();

            // 假如消费完 13 了，要申请 2 个，bufferSize = 8
            // 那么 current = 13, next = 15, wrapPoint = 7
            // 消费者的进度未知，生产者在申请第二轮的第六个和第七个位置，那么这两个位置的第一轮数据一定要已经消费
            // 也就是消费者的进度要 大于 7，即 cacheGatingSequence > wrapPoint。
            // 但是如果没有大于7呢？那么生产者就要阻塞等待消费者把这里的数据消费掉
            //
            if (wrapPoint > cacheGatingSequence || cacheGatingSequence > current) {
                long gatingSequence = Util.getMinimumSequence(gatingSequences, current);
                if (wrapPoint > gatingSequence) {
                    LockSupport.parkNanos(1);
                    continue;
                }
                // 能走到这里就是 wrapPoint > gatingSequence, 将最新的消费者进度更新一下
                gatingSequenceCache.set(gatingSequence);
            } else if (cursor.compareAndSet(current, next)) {
                // 走到这里说明不会发生覆盖数据的情况，使用 CAS 去申请序号 👆
                break;
            }
        } while (true);
        return next;
    }


}
