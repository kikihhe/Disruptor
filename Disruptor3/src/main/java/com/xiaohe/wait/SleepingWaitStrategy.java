package com.xiaohe.wait;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.ProcessingSequenceBarrier;

import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 16:40
 */
public class SleepingWaitStrategy implements WaitStrategy{

    private static final int DEFAULT_RETRIES = 200;
    public static final long DEFAULT_SLEEP = 100;

    private int retries;

    private final long sleepTimeNs;

    public SleepingWaitStrategy() {
        this(DEFAULT_RETRIES, DEFAULT_SLEEP);
    }

    public SleepingWaitStrategy(int retries, long sleepTimeNs) {
        this.retries = retries;
        this.sleepTimeNs = sleepTimeNs;
    }


    @Override
    public long waitFor(final long sequence, Sequence cursor, Sequence dependentSequence, ProcessingSequenceBarrier sequenceBarrier) {
        long availableSequence = 0;
        int counter = retries;
        // 如果生产者的进度一直小于消费者想要消费的进度，那就一直阻塞
        while ((availableSequence = dependentSequence.get()) < sequence) {
            counter = applyWaitMethod(counter);
        }
        return availableSequence;
    }

    private int applyWaitMethod(int counter) {
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

    public void setRetries(int retries) {
        this.retries = retries;
    }
}
