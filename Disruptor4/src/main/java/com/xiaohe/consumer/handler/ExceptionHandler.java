package com.xiaohe.consumer.handler;

public interface ExceptionHandler<T> {

    void handleEventException(Throwable ex, long sequence, T event);


    void handleOnStartException(Throwable ex);


    void handleOnShutdownException(Throwable ex);
}