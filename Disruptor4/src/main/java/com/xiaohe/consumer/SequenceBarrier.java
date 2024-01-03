package com.xiaohe.consumer;

import com.xiaohe.exception.AlertException;

import java.util.concurrent.TimeoutException;

/**
 * @author : 小何
 * @Description : 序号屏障
 * @date : 2024-01-01 14:00
 */
public interface SequenceBarrier {
    long waitFor(long sequence) throws AlertException, InterruptedException, TimeoutException;

    long getCursor();

    boolean isAlerted();
    void alert();

    void clearAlert();

    void checkAlert() throws AlertException;

}
