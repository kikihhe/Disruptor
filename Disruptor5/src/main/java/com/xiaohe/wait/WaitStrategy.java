package com.xiaohe.wait;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.exception.AlertException;
import com.xiaohe.exception.TimeoutException;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-02-06 12:15
 */
public interface WaitStrategy {
    public long waitFor(long sequence,
                        Sequence cursor,
                        Sequence dependentSequence,
                        SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException;

    public void signalAllWhenBlocking();
}
