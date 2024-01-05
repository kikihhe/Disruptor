package com.xiaohe;

import com.xiaohe.common.EventFactory;
import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.*;
import com.xiaohe.consumer.batch.BatchEventProcessor;
import com.xiaohe.consumer.batch.EventHandler;
import com.xiaohe.consumer.batch.EventHandlerGroup;
import com.xiaohe.consumer.handler.ExceptionHandler;
import com.xiaohe.consumer.handler.ExceptionHandlerWrapper;
import com.xiaohe.consumer.multi.WorkHandler;
import com.xiaohe.consumer.multi.WorkerPool;
import com.xiaohe.exception.TimeoutException;
import com.xiaohe.provider.ProducerType;
import com.xiaohe.provider.translator.EventTranslatorOneArg;
import com.xiaohe.util.Util;
import com.xiaohe.util.wait.WaitStrategy;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-05 17:08
 */
public class Disruptor<T> {
    /**
     * 环形数组
     */
    private final RingBuffer<T> ringBuffer;

    /**
     * 消费者是一个 Runnable，使用线程池将它封装为 Thread 并执行
     */
    private final Executor executor;

    /**
     * 存放所有消费者
     */
    private final ConsumerRepository<T> consumerRepository = new ConsumerRepository<>();
    /**
     * Disruptor是否已经启动
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    private ExceptionHandler<? super T> exceptionHandler = new ExceptionHandlerWrapper<>();

    public Disruptor (final EventFactory<T> eventFactory,
                      final int ringBufferSize,
                      final ThreadFactory threadFactory,
                      final ProducerType producerType,
                      final WaitStrategy waitStrategy) {

        this(RingBuffer.create(producerType, eventFactory, ringBufferSize, waitStrategy),
                new BasicExecutor(threadFactory));
    }

    private Disruptor(final RingBuffer<T> ringBuffer, final Executor executor) {
        this.ringBuffer = ringBuffer;
        this.executor = executor;
    }
    public RingBuffer<T> start() {
        checkOnlyStartedOnce();
        // 循环启动所有消费者
        for (final ConsumerInfo consumerInfo : consumerRepository) {
            consumerInfo.start(executor);
        }
        return ringBuffer;
    }

    /**
     * 发布数据
     * @param eventTranslator
     * @param arg
     * @param <A>
     */
    public <A> void publishEvent(final EventTranslatorOneArg<T, A> eventTranslator, final A arg) {
        ringBuffer.publishEvent(eventTranslator, arg);
    }
    @SuppressWarnings("varargs")
    @SafeVarargs
    public final EventHandlerGroup<T> handleEventsWith(final EventHandler<? super T>... handlers) {
        return createEventProcessors(new Sequence[0], handlers);
    }
    public EventHandlerGroup<T> handleEventsWith(final EventProcessor...processors) {
        for (EventProcessor processor : processors) {
            consumerRepository.add(processor);
        }
        // 超级一个存放消费者进度的数组，把消费者的进度放进去，然后把数组交给RingBuffer
        Sequence[] sequences = new Sequence[processors.length];
        for (int i = 0; i < processors.length; i++) {
            sequences[i] = processors[i].getSequence();
        }
        ringBuffer.addGatingSequences(sequences);
        return new EventHandlerGroup<>(this, consumerRepository, Util.getSequencesFor(processors));
    }

    private EventHandlerGroup<T> createEventProcessors(final Sequence[] barrierSequences,
                                                       final EventProcessorFactory<T>[] processorFactories) {
        final EventProcessor[] eventProcessors = new EventProcessor[processorFactories.length];
        for (int i = 0; i < processorFactories.length; i++) {
            eventProcessors[i] = processorFactories[i].createEventProcessor(ringBuffer, barrierSequences);
        }
        return handleEventsWith(eventProcessors);
    }

    /**
     *
     * @param barrierSequences 有的消费者是在别的消费者后面消费的，也就是顺序消费。
     * @param eventHandlers 所有消费者
     * @return
     */
    public EventHandlerGroup<T> createEventProcessors(final Sequence[] barrierSequences,
                                                      final EventHandler<? super T>[] eventHandlers) {
        checkNotStarted();
        // 根据提供的消费者长度，定义一个消费者进度数组，这个进度数组后面会赋值
        final Sequence[] processorSequences = new Sequence[eventHandlers.length];
        // 创建序号屏障
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier(barrierSequences);
        // 把用户定义的消费者行为 EventHandler 转换为 EventProcessor
        for (int i = 0; i < eventHandlers.length; i++) {
            final EventHandler<? super T> eventHandler = eventHandlers[i];
            BatchEventProcessor<T> batchEventProcessor = new BatchEventProcessor<>(ringBuffer, sequenceBarrier, eventHandler);
            if (exceptionHandler != null) {
                batchEventProcessor.setExceptionHandler(exceptionHandler);
            }
            consumerRepository.add(batchEventProcessor, eventHandler, sequenceBarrier);
            processorSequences[i] = batchEventProcessor.getSequence();
        }
        // 由于添加了新的消费者，所以最慢的消费者应该更新一下。
        updateGatingSequencesForNextInChain(barrierSequences, processorSequences);
        return new EventHandlerGroup<>(this, consumerRepository, processorSequences);
    }
    private void updateGatingSequencesForNextInChain(Sequence[] barrierSequences, Sequence[] processorSequences) {
        if (processorSequences.length > 0) {
            ringBuffer.addGatingSequences(processorSequences);
            // 这些消费者成为了新添加的消费者的前置消费者，就可以不用观察这些消费者的消费进度了
            // 只用观察最后消费的消费者进度即可，为什么这么说，比如说最后的消费者的消费进度是5，这就意味着前面的消费者肯定已经
            // 把环形数组5号位置的数据消费了，所以，只用看最后的消费者即可
            for (final Sequence barrierSequence : barrierSequences) {
                ringBuffer.removeGatingSequence(barrierSequence);
            }
            // 把这些消费者的最慢消费者身份更改掉，表示它不是最慢消费者了
            consumerRepository.unMarkEventProcessorsAsEndOfChain(barrierSequences);
        }
    }

    /**
     * 并发消费，且不存在依赖关系。
     * @param workHandlers
     * @return
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final EventHandlerGroup<T> handleEventsWithWorkerPool(final WorkHandler<T> ... workHandlers) {
        return createWorkerPool(new Sequence[0], workHandlers);
    }
    private EventHandlerGroup<T> createWorkerPool(final Sequence[] barrierSequences,
                                                  final WorkHandler<? super T>[] workHandlers) {
        SequenceBarrier sequenceBarrier = ringBuffer.newBarrier(barrierSequences);
        final WorkerPool<T> workerPool = new WorkerPool<>(ringBuffer, sequenceBarrier, exceptionHandler, workHandlers);
        // 把消费者的信息装进仓库
        consumerRepository.add(workerPool, sequenceBarrier);
        // 得到所有消费者的消费进度
        final Sequence[] workerSequences = workerPool.getWorkerSequences();
        updateGatingSequencesForNextInChain(barrierSequences, workerSequences);
        return new EventHandlerGroup<>(this, consumerRepository, workerSequences);
    }


    /**
     * 顺序消费
     * @param handlers 消费顺序在前的handler。其余的Handler在它们之后才进行消费
     * @return
     */
    @SafeVarargs
    @SuppressWarnings("varargs")
    public final EventHandlerGroup<T> after(final EventHandler<T>... handlers) {
        Sequence[] sequences = new Sequence[handlers.length];
        for (int i = 0, handlersLength = handlers.length; i < handlersLength; i++) {
            sequences[i] = consumerRepository.getSequenceFor(handlers[i]);
        }
        return new EventHandlerGroup<>(this, consumerRepository, sequences);
    }
    public EventHandlerGroup<T> after(final EventProcessor... processors) {
        for (final EventProcessor processor : processors) {
            consumerRepository.add(processor);
        }

        return new EventHandlerGroup<>(this, consumerRepository, Util.getSequencesFor(processors));
    }
    /**
     * 设置异常处理器
     * @param exceptionHandler
     */
    @SuppressWarnings("unchecked")
    public void setDefaultExceptionHandler(final ExceptionHandler<? super T> exceptionHandler) {
        checkNotStarted();
        if (!(this.exceptionHandler instanceof ExceptionHandlerWrapper)) {
            throw new IllegalStateException("setDefaultExceptionHandler can not be used after handleExceptionsWith");
        }
        ((ExceptionHandlerWrapper<T>) this.exceptionHandler).switchTo(exceptionHandler);
    }
    public void handleExceptionsWith(final ExceptionHandler<? super T> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }

    public void halt() {
        for (final ConsumerInfo consumerInfo : consumerRepository) {
            consumerInfo.halt();
        }
    }
    public RingBuffer<T> getRingBuffer() {
        return ringBuffer;
    }

    /**
     * 得到当前的生产者序号
     * @return
     */
    public long getCursor() {
        return ringBuffer.getCursor();
    }

    //根据序列号取出环形数组对应的生产者事件
    public T get(final long sequence) {
        return ringBuffer.get(sequence);
    }


    /**
     * 根据handler获得消费者的序号屏障
     * @param handler
     * @return
     */
    public SequenceBarrier getBarrierFor(final EventHandler<T> handler) {
        return consumerRepository.getBarrierFor(handler);
    }

    /**
     * 通过handler来获得对应的消费者线程的消费进度
     * @param b1
     * @return
     */
    public long getSequenceValueFor(final EventHandler<T> b1) {
        return consumerRepository.getSequenceFor(b1).get();
    }

    public void shutdown() {
        try {
            shutdown(-1, TimeUnit.MILLISECONDS);
        } catch (final TimeoutException e) {
            exceptionHandler.handleOnShutdownException(e);
        }
    }
    public void shutdown(final long timeout, final TimeUnit timeUnit) throws TimeoutException {
        final long timeoutAt = System.currentTimeMillis() + timeUnit.toMillis(timeout);
        // 如果还有没消费的消息，就要继续等待
        while (hasBacklog()) {
            if (timeout > 0 && System.currentTimeMillis() > timeoutAt) {
                throw TimeoutException.INSTANCE;
            }
        }
        halt();
    }
    private boolean hasBacklog() {
        // 拿到生产者进度
        final long cursor = ringBuffer.getCursor();
        // 遍历所有消费者，只要有一个消费者进度小于生产者进度，那就代表还没有消费完
        for (final Sequence consumer : consumerRepository.getLastSequenceInChain(false)) {
            if (cursor > consumer.get()) {
                return true;
            }
        }
        return false;
    }
    /**
     * 检查是否已经启动过
     */
    private void checkNotStarted() {
        if (started.get()) {
            throw new IllegalStateException("All event handlers must be added before calling starts.");
        }
    }

    /**
     * 启动，使用 CAS 将 started 从 false 改成 true.
     */
    private void checkOnlyStartedOnce() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Disruptor.start() must only be called once.");
        }
    }


    @Override
    public String toString() {
        return "Disruptor{" +
                "ringBuffer=" + ringBuffer +
                ", started=" + started +
                ", executor=" + executor +
                '}';
    }
}
