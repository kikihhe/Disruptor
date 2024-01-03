package com.xiaohe;

import com.xiaohe.common.EventFactory;
import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.exception.InsufficientCapacityException;
import com.xiaohe.provider.*;
import com.xiaohe.util.Util;
import com.xiaohe.util.wait.WaitStrategy;
import com.xiaohe.util.wait.impl.BlockWaitStrategy;
import com.xiaohe.util.wait.impl.SleepingWaitStrategy;
import jdk.nashorn.internal.runtime.SpillProperty;
import sun.misc.Unsafe;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-03 15:44
 */
abstract class RingBufferPad {
    protected long p1, p2, p3, p4, p5, p6, p7;
}
abstract class RingBufferFields<E> extends RingBufferPad {
    private static final int BUFFER_PAD;

    private static final long REF_ARRAY_BASE;

    private static final int REF_ELEMENT_SHIFT;
    /**
     * 用Unsafe类的对象操纵数组
     */
    private static final Unsafe UNSAFE = Util.getUnsafe();

    static {
        // 计算出数组元素的引用占多少字节，一般为4
        final int scale = UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale) {
            REF_ELEMENT_SHIFT = 2;
        } else if (8 == scale) {
            REF_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size");
        }
        // 计算一下需要填充多少个无效数据（元素引用），默认为32个
        BUFFER_PAD = 128 / scale;
        // BUFFER_PAD << REF_ELEMENT_SHIFT 这行代码的意思就是填充32个空数据，每个数据是4字节，所以左移2位，就是乘以4，得到的就是128
        // UNSAFE.arrayBaseOffset(Object[].class) 获取整个数组的内存地址首位
        // 数组起始位置 + 无效字节数量 = 数组有效数据的起始位置
        REF_ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << REF_ELEMENT_SHIFT);
    }

    private final long indexMask;

    /**
     * 环形数组
     */
    private final Object[] entries;

    /**
     * 环形数组的容量，必须是二次幂
     */
    protected final int bufferSize;

    /**
     * 序号生成器
     */
    protected final Sequencer sequencer;

    public RingBufferFields(EventFactory<E> eventFactory, Sequencer sequencer) {
        this.sequencer = sequencer;
        this.bufferSize = sequencer.getBufferSize();
        this.indexMask = bufferSize - 1;
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must not be less than 1");
        }
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }
        this.entries = new Object[sequencer.getBufferSize() + 2 * BUFFER_PAD];
        // 填充数据
        fill(eventFactory);
    }
    private void fill(EventFactory<E> eventFactory) {
        for (int i = 0; i< bufferSize; i++) {
            entries[BUFFER_PAD+i] = eventFactory.newInstance();
        }
    }

    /**
     * 根据位置取出数据
     * @param sequence
     * @return
     */
    protected final E elementATt(long sequence) {
        return (E) UNSAFE.getObject(entries, REF_ARRAY_BASE + ((sequence & indexMask) << REF_ELEMENT_SHIFT));
    }
}


public class RingBuffer<E> extends RingBufferFields<E> implements Cursored, EventSequencer<E>, EventSink<E> {
    public static final long INITIAL_CURSOR_VALUE = Sequence.INITIAL_VALUE;
    protected long p1, p2, p3, p4, p5, p6, p7;

    public RingBuffer(EventFactory<E> eventFactory, Sequencer sequencer) {
        super(eventFactory, sequencer);
    }

    public static <E> RingBuffer<E> createMultiProducer(EventFactory<E> factory, int bufferSize, WaitStrategy waitStrategy) {
        MultiProducerSequencer multiProducerSequencer = new MultiProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<>(factory, multiProducerSequencer);
    }

    public static <E> RingBuffer<E> createMultiProducer(EventFactory<E> factory, int bufferSize) {
        return createMultiProducer(factory, bufferSize, new BlockWaitStrategy());
    }

    public static <E> RingBuffer<E> createSingleProducer(EventFactory<E> eventFactory, int bufferSize, WaitStrategy waitStrategy) {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<>(eventFactory, sequencer);
    }
    public static <E> RingBuffer<E> createSingleProducer(EventFactory<E> eventFactory, int bufferSize) {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize, new BlockWaitStrategy());
        return new RingBuffer<>(eventFactory, sequencer);
    }

    public static <E> RingBuffer<E> create(ProducerType producerType, EventFactory<E> eventFactory, int bufferSize, WaitStrategy waitStrategy) {
        switch (producerType) {
            case SINGLE:
                return createSingleProducer(eventFactory, bufferSize, waitStrategy);
            case MULTI:
                return createMultiProducer(eventFactory, bufferSize, waitStrategy);
            default:
                throw new IllegalStateException(producerType.toString());
        }
    }

    public SequenceBarrier newBarrier(Sequence... sequencesToTrack) {
        return sequencer.newBarrier(sequencesToTrack);
    }

    /**
     * 根据序号获得环形数组中对应的元素
     * @param sequence
     * @return
     */
    @Override
    public E get(long sequence) {
        return elementATt(sequence);
    }

    @Override
    public void publishEvent(EventTranslator<E> translator) {
        // 获得一个可用的序号
        final long sequence = sequencer.next();
        translateAndPublish(translator, sequencer);
    }

    @Override
    public <A> void publishEvent(EventTranslatorOneArg<E, A> translator, A arg0) {
        final long sequence = sequencer.next();
        translateAndPublish(translator, sequence, arg0);
    }

    private void translateAndPublish(EventTranslator<E> translator, long sequence) {
        try {
            translator.translateTo(get(sequence), sequence);
        } finally {
            sequencer.publish(sequence);
        }
    }

    private <A> void translateAndPublish(EventTranslatorOneArg<E, A> translator, long sequence, A arg0) {
        try {
            translator.translateTo(get(sequence), sequence, arg0);
        } finally {
            sequencer.publish(sequence);
        }
    }
    @Override
    public void publish(long sequence) {
        sequencer.publish(sequence);
    }

    @Override
    public void publish(long lo, long hi) {
        sequencer.publish(lo, hi);
    }
    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * 是否可以申请指定数量的序号
     * @param requiredCapacity
     * @return
     */
    @Override
    public boolean hasAvailableCapacity(int requiredCapacity) {
        return sequencer.hasAvailableCapacity(requiredCapacity);
    }

    @Override
    public long remainingCapacity() {
        return sequencer.remainingCapacity();
    }

    @Override
    public long next() {
        return sequencer.next();
    }

    @Override
    public long next(int n) {
        return sequencer.next(n);
    }

    @Override
    public long tryNext() throws InsufficientCapacityException {
        return sequencer.tryNext();
    }

    @Override
    public long tryNext(int n) throws InsufficientCapacityException {
        return sequencer.tryNext(n);
    }
    @Override
    public long getCursor() {
        return sequencer.getCursor();
    }


    /**
     * 把添加进来的消费者序号放入 gatingSequences 中
     * @param gatingSequence
     */
    public void addGatingSequences(Sequence... gatingSequence) {
        sequencer.addGatingSequences(gatingSequence);
    }

    /**
     * 得到所有消费者和当前生产者序号中最小的值
     * @return
     */
    public long getMinimumGatingSequence() {
        return sequencer.getMinimumSequence();
    }

    /**
     * 删除指定的消费者序号
     * @param sequence
     * @return
     */
    public boolean removeGatingSequence(Sequence sequence) {
        return sequencer.removeGatingSequence(sequence);
    }

    @Override
    public String toString() {
        return "RingBuffer{" +
                "bufferSize=" + bufferSize +
                ", sequencer=" + sequencer +
                "}";
    }
}
