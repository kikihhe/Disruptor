package com.xiaohe.consumer;

/**
 * @author : 小何
 * @Description : 消费者处理器，用于处理生产者发布的数据。用户实现这个接口，处理数据
 * @date : 2023-12-26 20:55
 */
public interface EventHandler<T> {
    public void onEvent(T event, long sequence);
}
