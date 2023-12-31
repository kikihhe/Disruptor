package com.xiaohe.handler;

import com.xiaohe.consumer.EventHandler;
import com.xiaohe.provider.Request;

/**
 * @author : 小何
 * @Description : 消费者逻辑
 * @date : 2023-12-26 22:37
 */
public class SimpleEventHandler<T> implements EventHandler<Request<T>> {
    @Override
    public void onEvent(Request<T> event, long sequence) {
        System.out.println("消费者线程" + Thread.currentThread().getName() + "拿到了event: " + event);
    }
}
