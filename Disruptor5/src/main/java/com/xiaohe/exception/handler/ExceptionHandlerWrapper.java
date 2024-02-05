package com.xiaohe.exception.handler;

import com.xiaohe.exception.handler.impl.FatalExceptionHandler;

/**
 * @author : 小何
 * @Description : 通过 switchTo 方法可以替换默认的FatalExceptionHandler
 * @date : 2024-02-05 21:55
 */
public class ExceptionHandlerWrapper<T> implements ExceptionHandler<T> {
    private ExceptionHandler<? super T> delegate = new FatalExceptionHandler();

    public void switchTo(final ExceptionHandler<? super T> exceptionHandler) {
        this.delegate = exceptionHandler;
    }
    @Override
    public void handleEventException(Throwable t, long sequence, T event) {
        delegate.handleEventException(t,sequence,event);
    }

    @Override
    public void handleOnStartException(Throwable t) {
        delegate.handleOnStartException(t);
    }

    @Override
    public void handleOnShutdownException(Throwable t) {
        delegate.handleOnShutdownException(t);
    }
}
