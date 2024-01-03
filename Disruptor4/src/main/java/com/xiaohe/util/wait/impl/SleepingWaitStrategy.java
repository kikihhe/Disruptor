package com.xiaohe.util.wait.impl;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.exception.AlertException;
import com.xiaohe.util.wait.WaitStrategy;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-01 13:37
 */
public class SleepingWaitStrategy implements WaitStrategy {
    //默认的自旋次数
    private static final int DEFAULT_RETRIES = 200;
    //默认的睡眠时间
    private static final long DEFAULT_SLEEP = 100;

    private final int retries;
    private final long sleepTimeNs;

    //该构造方法会被调用
    public SleepingWaitStrategy() {
        this(DEFAULT_RETRIES, DEFAULT_SLEEP);
    }

    public SleepingWaitStrategy(int retries) {
        this(retries, DEFAULT_SLEEP);
    }

    public SleepingWaitStrategy(int retries, long sleepTimeNs) {
        this.retries = retries;
        this.sleepTimeNs = sleepTimeNs;
    }

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException {
        long availableSequence;
        int counter = retries;
        while ((availableSequence = dependentSequence.get()) < sequence) {
            counter = applyWaitMethod(barrier, counter);
        }
        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking() {

    }

    private int applyWaitMethod(final SequenceBarrier sequenceBarrier, int counter) throws AlertException {
        // 检查消费者是否被中止了
        sequenceBarrier.checkAlert();
        if (counter > 100) {
            counter--;
        } else if (counter > 0) {
            counter--;
            Thread.yield();
        } else {
            LockSupport.parkNanos(sleepTimeNs);
        }
        return counter;
    }
}
