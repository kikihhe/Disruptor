package com.xiaohe.consumer.impl;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.EventHandler;
import com.xiaohe.consumer.EventProcessor;
import com.xiaohe.consumer.ProcessingSequenceBarrier;
import com.xiaohe.provider.DataProvider;
import sun.misc.Contended;

/**
 * @author : 小何
 * @Description : 消费者批处理器
 * @date : 2023-12-26 20:58
 */
public class BatchEventProcessor<T> implements EventProcessor {
    /**
     * 用户定义的消费逻辑
     */
    private final EventHandler<? super T> eventHandler;
    /**
     * 消费者自身的消费进度（已消费的下标）
     */
    private final Sequence sequence = new Sequence(-1);

    /**
     * RingBuffer 的父类，只提供了 get方法用于获取数据
     */
    private final DataProvider<T> dataProvider;

    private final ProcessingSequenceBarrier sequenceBarrier;

    public BatchEventProcessor(EventHandler<? super T> eventHandler, DataProvider<T> dataProvider, ProcessingSequenceBarrier sequenceBarrier) {
        this.eventHandler = eventHandler;
        this.dataProvider = dataProvider;
        this.sequenceBarrier = sequenceBarrier;
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void run() {
        processEvents();
    }

    public void processEvents() {
        T event = null;
        // 获取消费者自身应该消费的下一个数据的下标
        long nextSequence = sequence.get() + 1L;
        while (true) {
            // 拿着消费者想要消费的下标跟序号屏障中的生产者进度做对比，看看最大能消费多少号
            long availableSequence = sequenceBarrier.waitFor(nextSequence);
            // 批处理可以消费的数据
            while (nextSequence <= availableSequence) {
                event = dataProvider.get(nextSequence);
                eventHandler.onEvent(event, nextSequence);
                nextSequence++;
            }
            // 一次批量消费结束就更新进度
            sequence.setValue(availableSequence);
        }
    }
}
