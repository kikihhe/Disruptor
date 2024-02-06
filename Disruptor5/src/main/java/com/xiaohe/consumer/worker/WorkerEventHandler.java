package com.xiaohe.consumer.worker;

/**
 * @author : 小何
 * @Description : 并发消费者的处理器。当用户使用并发消费模式时，需要实现该接口
 * @date : 2024-02-06 12:29
 */
public interface WorkerEventHandler<T> {
    public void onEvent(T event) throws Exception;
}
