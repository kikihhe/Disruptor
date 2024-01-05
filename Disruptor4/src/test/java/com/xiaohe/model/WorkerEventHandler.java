package com.xiaohe.model;

import com.xiaohe.consumer.multi.WorkHandler;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-05 20:59
 */
public class WorkerEventHandler<T> implements WorkHandler<Event<T>> {

    @Override
    public void onEvent(Event<T> event) throws Exception {
        System.out.println("Thread Name - " + Thread.currentThread().getName()
                + "消费了 " + event.getData() + ".");
    }
}
