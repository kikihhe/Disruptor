package com.xiaohe.consumer.worker;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.EventProcessor;

/**
 * @author : 小何
 * @Description : 并发消费者模式
 * @date : 2024-02-06 12:27
 */
public class WorkerEventProcessor<T> implements EventProcessor {
    @Override
    public Sequence getSequence() {
        return null;
    }

    @Override
    public void halt() {

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public void run() {

    }
}
