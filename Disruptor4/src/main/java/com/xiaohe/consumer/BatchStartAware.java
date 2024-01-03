package com.xiaohe.consumer;

public interface BatchStartAware {
    //参数就是这次一共要消费的事件的个数
    void onBatchStart(long batchSize);
}