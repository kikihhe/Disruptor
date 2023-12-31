package com.xiaohe.provider;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.ProcessingSequenceBarrier;
import com.xiaohe.wait.WaitStrategy;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 16:25
 */
public class RingBuffer<E> implements DataProvider<E> {
    /**
     * 掩码，数组长度-1
     */
    private final long indexMask;

    private final Object[] entries;
    /**
     * 环形数组的容量
     */
    protected final int bufferSize;

    /**
     * 生产者序号生成器，用于给生产者分配可用序号的。
     */
    protected final SingleProducerSequencer sequencer;

    /**
     * @param eventFactory 创建数据的工厂
     * @param sequencer    序号生成器
     */
    public RingBuffer(EventFactory<E> eventFactory, SingleProducerSequencer sequencer) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must not be less than 1");
        }
        if (Integer.bitCount(bufferSize) != 1) {
            //数组容量不符合要求就直接抛异常
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }
        // 序号生成器赋值成功
        this.sequencer = sequencer;
        // 数组容量赋值
        this.bufferSize = bufferSize;
        // 给掩码赋值
        this.indexMask = bufferSize - 1;
        // 初始化数组
        this.entries = new Object[bufferSize];
        // 初始化数组，创建数组中的对象，这个对象可以一直复用，在一定程度上减少垃圾回收
        // 但是该对象中封装的对象，仍然会被垃圾回收，这个要搞清楚
        fill(eventFactory);
    }

    /**
     * 创建环形数组
     * 创建环形数组前创建序号生成器，序号生产器中含有
     * @param factory
     * @param bufferSize
     * @param waitStrategy
     * @return
     * @param <E>
     */
    public static <E> RingBuffer<E> create(EventFactory<E> factory, int bufferSize, WaitStrategy waitStrategy) {
        return createSingleProducer(factory, bufferSize, waitStrategy);
    }


    /**
     * 先创建序号生成器，再根据序号生成器创建RingBuffer
     * @param factory
     * @param bufferSize
     * @param waitStrategy
     * @return
     * @param <E>
     */
    public static <E> RingBuffer<E> createSingleProducer(EventFactory<E> factory, int bufferSize, WaitStrategy waitStrategy) {
        SingleProducerSequencer sequencer = new SingleProducerSequencer(bufferSize, waitStrategy);
        return new RingBuffer<>(factory, sequencer);
    }

    /**
     * 通过 生产者的序号生成器 创建 消费者的序号屏障
     * @return
     */
    public ProcessingSequenceBarrier newBarrier() {
        return sequencer.newBarrier();
    }

    /**
     * 把消费者的进度设置到生产者的序号生成器中
     * @param sequence
     */
    public void addGatingSequences(Sequence sequence) {
        sequencer.addGatingSequences(sequence);
    }

    /**
     * 将数组填充为无效的 Request 数据
     *
     * @param eventFactory
     */
    private void fill(EventFactory<E> eventFactory) {
        for (int i = 0; i < bufferSize; i++) {
            entries[i] = eventFactory.newInstance();
        }
    }
    public <A> void publishEvent(EventTranslatorOneArg<E, A> translator, A arg0) {
        // 获取下一个可用序号,只获取一个
        final long sequence = sequencer.next();
        // 给对应位置的Request填充上arg0
        translateAndPublish(translator, sequence, arg0);

    }

    /**
     * 将 arg0 塞到数组对应位置内，更新生产者进度
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

    protected final E elementAt(long sequence) {
        return (E) entries[(int) (sequence & indexMask)];
    }
}
