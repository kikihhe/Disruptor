package com.xiaohe;

import com.xiaohe.event.EventFactory;
import com.xiaohe.event.EventHandler;
import com.xiaohe.event.EventTranslatorOneArg;
import com.xiaohe.event.impl.BatchEventProcessor;
import com.xiaohe.pojo.Sequence;
import com.xiaohe.strategy.WaitStrategy;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-11-11 13:27
 */
public class Disruptor<T> {
    private final RingBuffer<T> ringBuffer;

    /**
     * 创建消费者线程的executor, BasicExecutor
     */
    private final Executor executor;

    /**
     * 包装后的消费者对象
     */
    BatchEventProcessor batchEventProcessor;

    /**
     * Disruptor启动状态的标记
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    public Disruptor(final EventFactory<T> eventFactory, final int ringBufferSize, final ThreadFactory threadFactory, final WaitStrategy waitStrategy){
        this(RingBuffer.create(eventFactory, ringBufferSize, waitStrategy), new BasicExecutor(threadFactory));
    }
    private Disruptor(final RingBuffer<T> ringBuffer, final Executor executor) {
        this.ringBuffer = ringBuffer;
        this.executor = executor;
    }

    /**
     * 把用户定义的消费任务包装为Runnable。这些任务会交给线程来执行
     * 这些方法的参数是个可变参数，用户可能会定义多个消费者事件，即多个消费者
     * @param handlers
     */
    public final void handleEventsWith(final EventHandler<? super T>... handlers) {
        createEventProcessors(handlers);
    }

    void createEventProcessors(EventHandler<? super T>[] handlers) {
        // 检查Disruptor是否启动过，因为要保证Disruptor只能成功启动一次
        checkNotStarted();
        // 定义消费者进度对象
        Sequence sequence = null;
        final ProcessingSequenceBarrier barrier = ringBuffer.newBarrier();
        for(int i = 0, eventHandlersLength = handlers.length; i < eventHandlersLength; i++) {
            final EventHandler<? super T> eventHandler = handlers[i];
            batchEventProcessor = new BatchEventProcessor(ringBuffer, barrier, eventHandler);
            sequence = batchEventProcessor.getSequence();
        }
        ringBuffer.addGatingSequence(sequence);
    }

    private void checkNotStarted() {
        if (started.get()) {
            throw new IllegalStateException("All event handler must be added before calling started.");
        }
    }
    public <A> void publishEvent(final EventTranslatorOneArg<T, A> eventTranslatorOneArg, final A arg) {
        ringBuffer.publishEvent(eventTranslatorOneArg, arg);
    }

    public RingBuffer<T> start() {
        checkOnlyStartedOnce();
        executor.execute(batchEventProcessor);
        return ringBuffer;
    }
    private void checkOnlyStartedOnce() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Disruptor.start() must only be called once.");
        }
    }

}
