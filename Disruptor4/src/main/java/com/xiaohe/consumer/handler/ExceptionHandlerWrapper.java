package com.xiaohe.consumer.handler;

public class ExceptionHandlerWrapper<T> implements ExceptionHandler<T> {
    // FatalExceptionHandler是程序内部默认的异常处理器，这个处理器处理异常的程度太强烈
    // 直接就终止程序了，所以一般来说都是用户定义一个异常处理器，然后替换程序内部的默认处理器来处理程序的异常
    private ExceptionHandler<? super T> delegate = new FatalExceptionHandler();

    //这个就是替换处理器的方法
    public void switchTo(final ExceptionHandler<? super T> exceptionHandler) {
        this.delegate = exceptionHandler;
    }

    @Override
    public void handleEventException(final Throwable ex, final long sequence, final T event) {
        delegate.handleEventException(ex, sequence, event);
    }

    @Override
    public void handleOnStartException(final Throwable ex) {
        delegate.handleOnStartException(ex);
    }

    @Override
    public void handleOnShutdownException(final Throwable ex) {
        delegate.handleOnShutdownException(ex);
    }
}