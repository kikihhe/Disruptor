package com.xiaohe.consumer;

import com.xiaohe.common.Sequence;

import java.util.concurrent.Executor;

/**
 * 消费者池的信息
 * @param <T>
 */
class WorkerPoolInfo<T> implements ConsumerInfo {
    //消费者池
    private final WorkerPool<T> workerPool;
    //序号屏障
    private final SequenceBarrier sequenceBarrier;

    private boolean endOfChain = true;

    WorkerPoolInfo(final WorkerPool<T> workerPool, final SequenceBarrier sequenceBarrier) {
        this.workerPool = workerPool;
        this.sequenceBarrier = sequenceBarrier;
    }

    @Override
    public Sequence[] getSequences() {
        return workerPool.getWorkerSequences();
    }

    @Override
    public SequenceBarrier getBarrier() {
        return sequenceBarrier;
    }

    @Override
    public boolean isEndOfChain() {
        return endOfChain;
    }

    @Override
    public void start(Executor executor) {
        workerPool.start(executor);
    }

    @Override
    public void halt() {
        workerPool.halt();
    }

    @Override
    public void markAsUsedInBarrier() {
        endOfChain = false;
    }

    @Override
    public boolean isRunning() {
        return workerPool.isRunning();
    }
}
