package com.xiaohe.model;

import com.xiaohe.consumer.batch.EventHandler;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-05 20:53
 */
public class SimpleEventHandler<T> implements EventHandler<Event<T>> {

    public int index;

    public SimpleEventHandler(int index) {
        this.index = index;
    }

    @Override
    public void onEvent(Event<T> event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("Thread Name - " + Thread.currentThread().getName() + ", 第" + index + "个处理器, "
                + "消费了 " + event.getData() + ".");
    }
}
