package com.xiaohe.consumer;

import com.xiaohe.Sequence;
import com.xiaohe.provider.RingBuffer;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : 小何
 * @Description : 消费者池，内含很多消费者，所有的消费者都要来这个池子申请自己能消费的序号
 * @date : 2023-12-31 22:21
 */
public final class WorkerPool<T> {
    /**
     * 这个组件是否已经启动
     */
    private final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * 给消费者分配序号
     */
    private final Sequence workSequence = new Sequence(Sequence.INITIAL_VALUE);

    /**
     * 消费者
     */
    private final WorkProcessor<? extends T>[] workProcessors;
    /**
     * 环形数组
     */
    private final RingBuffer<T> ringBuffer;

    @SafeVarargs
    public WorkerPool(final RingBuffer<T> ringBuffer,
                      final ProcessingSequenceBarrier sequenceBarrier,
                      final ExceptionHandler<? extends T> exceptionHandler,
                      final WorkHandle<? extends T> ... workHandles) {
        this.ringBuffer = ringBuffer;
        // 消费者数量
        final int numWorkers = workHandles.length;
        workProcessors = new WorkProcessor[numWorkers];
        for (int i = 0; i < numWorkers; i++) {
            workProcessors[i] = new WorkProcessor<>(ringBuffer, sequenceBarrier, workHandles[i], exceptionHandler, workSequence);
        }
    }

    /**
     * 返回所有消费者的进度，并且将自身的进度也返回，放在尾部
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


    /**
     * 启动消费者池，也就是启动所有消费者，然后执行 workProcessor 方法
     * @param executor
     * @return
     */
    public RingBuffer<T> start(final Executor executor) {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("WorkerPool has already bean started");
        }
        // 得到当前生产者的生产进度
        final long cursor = ringBuffer.getCursor();
        // 消费者从当前的生产进度开始消费，所以，要先启动消费者，以防部分数据丢失
        workSequence.set(cursor);
        // 将可用序号分配给消费者，并启动所有消费者线程，
        for (WorkProcessor<? extends T> processor : workProcessors) {
            processor.getSequence().set(cursor);
            executor.execute(processor);
        }
        return ringBuffer;
    }
}
