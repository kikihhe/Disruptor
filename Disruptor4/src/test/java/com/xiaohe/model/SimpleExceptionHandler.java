package com.xiaohe.model;

import com.xiaohe.consumer.handler.ExceptionHandler;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-05 20:57
 */
public class SimpleExceptionHandler<T> implements ExceptionHandler<Event<T>> {

    @Override
    public void handleEventException(Throwable ex, long sequence, Event<T> event) {
        System.out.println("消息消费出现了异常" + ex + ", sequence: " + sequence + ", event: " + event);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        System.out.println("启动异常" + ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        System.out.println("结束异常" + ex);
    }
}
