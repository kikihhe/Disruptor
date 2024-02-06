package com.xiaohe.consumer.batch;

/**
 * @author : 小何
 * @Description : 批量消费者的处理器。当用户使用批量消费模式时，需要实现该接口
 * @date : 2024-02-06 12:26
 */
public interface BatchEventHandler<T> {
    public void onEvent(T event, long sequence, boolean endOfBatch) throws Exception;
}
