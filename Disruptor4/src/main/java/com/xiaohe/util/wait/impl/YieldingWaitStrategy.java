package com.xiaohe.util.wait.impl;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.exception.AlertException;
import com.xiaohe.util.wait.WaitStrategy;

import java.util.concurrent.TimeoutException;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-01 13:38
 */
public class YieldingWaitStrategy implements WaitStrategy {
    private static final int SPIN_TRIES = 100;

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException {
        long availableSequence;
        int counter = SPIN_TRIES;
        while ((availableSequence = dependentSequence.get()) < sequence) {
            counter = applyWaitMethod(barrier, counter);
        }
        return availableSequence;
    }

    private int applyWaitMethod(SequenceBarrier sequenceBarrier, int counter) throws AlertException {
        sequenceBarrier.checkAlert();
        if (counter > 0) {
            counter--;
        } else {
            Thread.yield();
        }
        return counter;
    }

    @Override
    public void signalAllWhenBlocking() {

    }
}
