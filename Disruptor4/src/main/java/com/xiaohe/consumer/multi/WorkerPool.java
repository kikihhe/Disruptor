package com.xiaohe.consumer.multi;

import com.xiaohe.RingBuffer;
import com.xiaohe.common.EventFactory;
import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.consumer.handler.ExceptionHandler;
import com.xiaohe.util.Util;
import com.xiaohe.util.wait.impl.BlockWaitStrategy;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-03 20:39
 */
public final class WorkerPool<T> {
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 用来给这个消费者池子中的每一个消费者分配序号的
     */
    private final Sequence workSequence = new Sequence(Sequence.INITIAL_VALUE);

    private final RingBuffer<T> ringBuffer;

    /**
     * 此消费者池中的所有消费者
     */
    private final WorkProcessor<?>[] workProcessors;

    @SafeVarargs
    public WorkerPool(final RingBuffer<T> ringBuffer,
                      final SequenceBarrier sequenceBarrier,
                      final ExceptionHandler<? super T> exceptionHandler,
                      final WorkHandler<? super T>... workHandlers) {
        this.ringBuffer = ringBuffer;
        final int numWorkers = workHandlers.length;
        workProcessors = new WorkProcessor[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            workProcessors[i] = new WorkProcessor<>(ringBuffer, sequenceBarrier, workHandlers[i], exceptionHandler, workSequence);
        }
    }

    @SafeVarargs
    public WorkerPool(final EventFactory<T> eventFactory,
                      final ExceptionHandler<? super T> exceptionHandler,
                      final WorkHandler<? super T> ... workHandlers) {
        ringBuffer = RingBuffer.createMultiProducer(eventFactory, 1024, new BlockWaitStrategy());
        final SequenceBarrier sequenceBarrier = ringBuffer.newBarrier();
        final int numWorkers = workHandlers.length;
        workProcessors = new WorkProcessor[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            workProcessors[i] = new WorkProcessor<>(ringBuffer, sequenceBarrier, workHandlers[i], exceptionHandler, workSequence);
        }
        ringBuffer.addGatingSequences(getWorkerSequences());;
    }

    /**
     * 获取消费者数组中的消费者进度
     * @return
     */
    public Sequence[] getWorkerSequences() {

        final Sequence[] sequences = new Sequence[workProcessors.length + 1];
        for (int i = 0, size = workProcessors.length; i < size; i++) {
            sequences[i] = workProcessors[i].getSequence();
        }
        sequences[sequences.length - 1] = workSequence;

        return sequences;
    }

    public RingBuffer<T> start(final Executor executor) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("WorkerPool has already bean started and cannot be restarted until halted");
        }
        final long cursor = ringBuffer.getCursor();
        workSequence.set(cursor);
        for (WorkProcessor<?> processor : workProcessors) {
            processor.getSequence().set(cursor);
            executor.execute(processor);
        }
        return ringBuffer;
    }

    /**
     * 停止所有消费者，前提是数据已经消费完了
     */
    public void drainAndHalt() {
        Sequence[] workerSequences = getWorkerSequences();
        while (ringBuffer.getCursor() > Util.getMinimumSequence(workerSequences)) {
            Thread.yield();
        }
        for (WorkProcessor<?> processor : workProcessors) {
            processor.halt();
        }
        started.set(false);
    }

    /**
     * 停止所有消费者，不管剩下的数据了
     */
    public void halt() {
        for (WorkProcessor<?> processor : workProcessors) {
            processor.halt();
        }
        started.set(false);
    }
    public boolean isRunning() {
        return started.get();
    }
}
