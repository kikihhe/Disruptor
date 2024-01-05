package com.xiaohe.consumer.aware;

/**
 * 每次批量消费前要执行的生命周期接口
 */
public interface BatchStartAware {

    /**
     * 批量执行的消费者每一次申请到序号都执行的方法
     * @param batchSize
     */
    void onBatchStart(long batchSize);
}