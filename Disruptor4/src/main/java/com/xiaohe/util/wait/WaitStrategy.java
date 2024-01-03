package com.xiaohe.util.wait;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.exception.AlertException;

import java.util.concurrent.TimeoutException;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-01 13:37
 */
public interface WaitStrategy {
    /**
     * 阻塞着获取可消费的最大进度
     * @param sequence
     * @param cursor
     * @param dependentSequence
     * @param barrier
     * @return
     * @throws AlertException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier)
            throws AlertException, InterruptedException, TimeoutException;


    void signalAllWhenBlocking();
}
