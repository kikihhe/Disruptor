package com.xiaohe.provider;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.ProcessingSequenceBarrier;
import com.xiaohe.provider.impl.SingleProducerSequencer;
import com.xiaohe.util.Util;
import com.xiaohe.wait.WaitStrategy;
import sun.misc.Unsafe;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 16:25
 */
abstract class RingBufferPad {
    protected long p1, p2, p3, p4, p5, p6, p7;
}

abstract class RingBufferFields<E> extends RingBufferPad {
    /**
     * 数组中无效数据的个数
     * 要根据数组中的元素的大小来判断，如果数组中的元素为int，那么这个值就是 128/4 = 32，
     * 数组的元素如果是 long，那就是 128 / 8 = 16
     */
    private static final int BUFFER_PAD;
    /**
     * 数组分为两部分 : 无效填充 + 有效数据，REF_ARRAY_BASE存储的是数组中第一个有效数据的偏移量
     */
    private static final long REF_ARRAY_BASE;
    /**
     * 计算数组应该填充的空白字节的大小，计算数组每一位的大小，因为数组中存放的都是对象引用，所以每一位的大小都为4
     */
    private static final int REF_ELEMENT_SHIFT;
    /**
     * 用于操作数组
     */
    private static final Unsafe UNSAFE = Util.getUnsafe();

    static {
        // 数组每一个位置存放的都是引用，都是4个字节
        final int scale = UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale) {
            REF_ELEMENT_SHIFT = 2;
        } else if (8 == scale) {
            REF_ELEMENT_SHIFT = 3;
        } else {
            throw new IllegalStateException("Unknown pointer size.");
        }
        BUFFER_PAD = 128 / scale;
        // 找到第一个有效元素
        // 整个数组的偏移量 + 无效字节填充，其实就是128
        // 为啥不直接加128？后面好扩展
        REF_ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << REF_ELEMENT_SHIFT);
    }

    /**
     * 用来进行位运算的数组掩码，indexMask = bufferSize - 1
     */
    private final long indexMask;
    /**
     * 存放元素的数组，无效填充 + 有效数据
     */
    private final Object[] entries;
    /**
     * 环形数组的容量，必须是2的次幂
     */
    protected final int bufferSize;

    /**
     * 序号生成器，用于给生产者分配可用序号的
     * SingleProducerSequencer, 它继承了 Sequence
     */
    protected final SingleProducerSequencer sequencer;

    public RingBufferFields(EventFactory<E> eventFactory, SingleProducerSequencer sequencer) {
        this.sequencer = sequencer;
        this.bufferSize = sequencer.getBufferSize();
        if (bufferSize < 1) {
            throw new IllegalStateException("bufferSize must not be less than 1");
        }
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalStateException("bufferSize must bea power of 2");
        }
        this.indexMask = bufferSize - 1;
        this.entries = new Object[sequencer.getBufferSize() + 2 * BUFFER_PAD];
        fill(eventFactory);
    }

    private void fill(EventFactory<E> eventFactory) {
        for (int i = 0; i < bufferSize; i++) {
            entries[BUFFER_PAD + i] = eventFactory.newInstance();
        }
    }

    /**
     * 根据序号得到数组中对应位置的数据
     * @param sequence
     * @return
     */
    protected final E elementAt(long sequence) {
        return (E) UNSAFE.getObject(entries, REF_ARRAY_BASE + (sequence & indexMask) << REF_ELEMENT_SHIFT);
    }
}

public class RingBuffer<E> extends RingBufferFields<E> implements  Cursored, DataProvider<E> {

    protected long p1, p2, p3, p4, p5, p6, p7;

    public RingBuffer(EventFactory<E> eventFactory, SingleProducerSequencer sequencer) {
        super(eventFactory, sequencer);
    }

    /**
     * 创建环形数组
     * 创建环形数组前创建序号生成器，序号生产器中含有
     *
     * @param factory
     * @param bufferSize
     * @param waitStrategy
     * @param <E>
     */
    public static <E> RingBuffer<E> create(EventFactory<E> factory, int bufferSize, WaitStrategy waitStrategy) {
        return createSingleProducer(factory, bufferSize, waitStrategy);
    }


    /**
     * 先创建序号生成器，再根据序号生成器创建RingBuffer
     *
     * @param factory
     * @param bufferSize
     * @param waitStrategy
     * @param <E>
     */
    public static <E> RingBuffer<E> createSingleProducer(EventFactory<E> factory, int bufferSize, WaitStrategy waitStrategy) {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<>(factory, sequencer);
    }

    /**
     * 通过 生产者的序号生成器 创建 消费者的序号屏障
     */
    public ProcessingSequenceBarrier newBarrier() {
        return sequencer.newBarrier();
    }

    /**
     * 把消费者的进度设置到生产者的序号生成器中
     *
     * @param sequence
     */
    public void addGatingSequences(Sequence sequence) {
        sequencer.addGatingSequences(sequence);
    }



    public <A> void publishEvent(EventTranslatorOneArg<E, A> translator, A arg0) {
        // 获取下一个可用序号,只获取一个
        final long sequence = sequencer.next();
        // 给对应位置的Request填充上arg0
        translateAndPublish(translator, sequence, arg0);

    }

    /**
     * 将 arg0 塞到数组对应位置内，更新生产者进度
     *
     * @param translator
     * @param sequence
     * @param arg0
     * @param <A>
     */
    private <A> void translateAndPublish(EventTranslatorOneArg<E, A> translator, long sequence, A arg0) {
        try {
            // 拿到对应元素，一般为 Request
            E request = get(sequence);
            // 将 arg0 塞到 Request 中
            translator.translateTo(request, sequence, arg0);
        } finally {
            sequencer.publish(sequence);
        }
    }


    /**
     * 获得生产者的下一个可用序号
     */
    public long next() {
        return 0;
    }

    public E get(long sequence) {
        return elementAt(sequence);
    }
}
