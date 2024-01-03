package com.xiaohe.util.wait.impl;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.exception.AlertException;
import com.xiaohe.util.wait.WaitStrategy;

import java.util.concurrent.TimeoutException;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-03 18:03
 */
public class BlockWaitStrategy implements WaitStrategy {
    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException {
        return 0;
    }

    @Override
    public void signalAllWhenBlocking() {

    }
}
