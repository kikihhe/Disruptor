package com.xiaohe.consumer;

import com.xiaohe.exception.AlertException;
import com.xiaohe.exception.TimeoutException;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-02-06 12:07
 */
public interface SequenceBarrier {
    public long waitFor(long sequence) throws AlertException, InterruptedException, TimeoutException;

    /**
     * 给出此消费者依赖的人的进度，可能是生产者，可能是其他消费者
     * @return
     */
    public long getCursor();

    public boolean isAlerted();

    public void alert();

    public void clearAlert();

    public void checkAlert() throws AlertException;
}
