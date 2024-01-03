package com.xiaohe.consumer;

/**
 * @author : 小何
 * @Description : 消费者要实现的接口，消费者的逻辑就定义在该类的方法中
 * @date : 2024-01-01 14:55
 */
public interface WorkHandler<T> {
    public void onEvent(T event) throws Exception;
}
