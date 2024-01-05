package com.xiaohe.consumer.batch;

/**
 * 消费者事件处理器，用户实现
 * @param <T>
 */
public interface EventHandler<T> {

    void onEvent(T event, long sequence, boolean endOfBatch) throws Exception;
}