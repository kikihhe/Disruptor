package com.xiaohe.consumer.aware;

/**
 * @author : 小何
 * @Description : 批量消费者在每次消费前都要执行的生命周期接口
 * @date : 2024-02-05 21:30
 */
public interface BatchStartAware {
    /**
     * 批量消费者每次申请到序号都执行的方法
     * @param batchSize
     */
    void onBatchStart(long batchSize);
}
