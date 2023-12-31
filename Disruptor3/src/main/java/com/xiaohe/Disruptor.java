package com.xiaohe;

import com.xiaohe.consumer.EventHandler;
import com.xiaohe.consumer.ProcessingSequenceBarrier;
import com.xiaohe.consumer.impl.BatchEventProcessor;
import com.xiaohe.provider.EventFactory;
import com.xiaohe.provider.EventTranslatorOneArg;
import com.xiaohe.provider.RingBuffer;
import com.xiaohe.wait.WaitStrategy;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-26 23:26
 */
public class Disruptor<T> {
    private final RingBuffer<T> ringBuffer;

    private final Executor executor;

    BatchEventProcessor batchEventProcessor;

    /**
     * Disruptor启动的标记，因为在整个程序中要确保Disruptor只启动一次
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    public Disruptor(final EventFactory<T> eventFactory,
                     final int bufferSize,
                     final ThreadFactory threadFactory,
                     final WaitStrategy waitStrategy) {
        // RingBuffer.create(eventFactory, bufferSize, waitStrategy) : 创建一个 RingBuffer
        // 创建 RingBuffer 时会创建 序号生成器
        // 创建 序号生成器 时会创建 生产者进度。此时序号生成器中还缺一个消费者进度
        this(RingBuffer.create(eventFactory, bufferSize, waitStrategy), new BasicExecutor(threadFactory));
    }

    public Disruptor(RingBuffer<T> ringBuffer, Executor executor) {
        this.ringBuffer = ringBuffer;
        this.executor = executor;
    }

    /**
     * 将用户提交的消费者事件包装为 batchEventProcessor
     * 创建时会给每一个消费者创建一个消费者进度, 并且根据 RingBuffer内部的序号生成器创建一个序号屏障
     * 消费者进度创建之后会由 RingBuffer 交给序号生成器
     * @param handlers
     */
    public final void handleEventsWith(final EventHandler<? super T> ... handlers) {
        createEventProcessors(handlers);
    }

    private void createEventProcessors(EventHandler<? super T>[] eventHandlers) {
        checkNotStarted();
        // 定义消费者进度
        Sequence sequence = null;
        // 创建消费者序号屏障
        final ProcessingSequenceBarrier barrier = ringBuffer.newBarrier();
        // 开始遍历所有handler
        int eventHandlerLength = eventHandlers.length;
        for (int i = 0; i < eventHandlerLength; i++) {
            final EventHandler<? super T> eventHandler = eventHandlers[i];
            // 每一个BatchEventProcessor对象都是一个消费者
            batchEventProcessor = new BatchEventProcessor(eventHandler, ringBuffer, barrier);
            sequence = batchEventProcessor.getSequence();
        }
        // 把消费者进度交给ringBuffer, ringBuffer会交给生产者的序号申请器
        ringBuffer.addGatingSequences(sequence);
    }

    public <A> void publishEvent(final EventTranslatorOneArg<T, A> eventTranslatorOneArg, final A arg) {
        ringBuffer.publishEvent(eventTranslatorOneArg, arg);
    }

    public RingBuffer<T> start() {
        checkOnlyStartedOnce();
        executor.execute(batchEventProcessor);
        return ringBuffer;
    }


    private void checkNotStarted() {
        if (started.get()) {
            throw new IllegalStateException();
        }
    }
    private void checkOnlyStartedOnce() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException();
        }
    }
}
