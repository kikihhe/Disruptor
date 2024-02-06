package com.xiaohe.consumer.batch;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.EventProcessor;

/**
 * @author : 小何
 * @Description : 批量消费者
 * @date : 2024-02-06 12:28
 */
public class BatchEventProcessor<T> implements EventProcessor {
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
