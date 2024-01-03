package com.xiaohe.exception.handler;

import com.xiaohe.common.Event;

public class SimpleExceptionHandler<T> implements ExceptionHandler<Event<T>> {

    //处理事件时出现异常的解决方法，为了简化，我就只打印一句话了
    @Override
    public void handleEventException(Throwable ex, long sequence, Event<T> event) {
        System.out.println("出现了异常了"+ex);
    }

    //启动程序时出现异常的解决方法
    @Override
    public void handleOnStartException(Throwable ex) {
        System.out.println("出现了异常了"+ex);
    }

    //终止程序时出现异常的解决方法
    @Override
    public void handleOnShutdownException(Throwable ex) {
        System.out.println("出现了异常了"+ex);
    }
}