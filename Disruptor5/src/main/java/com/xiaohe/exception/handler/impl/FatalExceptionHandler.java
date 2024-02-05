package com.xiaohe.exception.handler.impl;

import com.xiaohe.exception.handler.ExceptionHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * @author : 小何
 * @Description : 异常处理的默认实现，当出现异常时将异常抛出
 * @date : 2024-02-05 21:51
 */
public class FatalExceptionHandler implements ExceptionHandler<Object> {
    private static final Logger LOGGER = Logger.getLogger(FatalExceptionHandler.class.getName());
    private final Logger logger;

    public FatalExceptionHandler() {
        this.logger = LOGGER;
    }
    public FatalExceptionHandler(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void handleEventException(final Throwable ex, final long sequence, final Object event) {
        logger.log(Level.SEVERE, "Exception processing: " + sequence + " " + event, ex);
        throw new RuntimeException(ex);
    }

    @Override
    public void handleOnStartException(final Throwable ex) {
        logger.log(Level.SEVERE, "Exception during onStart()", ex);
    }

    @Override
    public void handleOnShutdownException(final Throwable ex) {
        logger.log(Level.SEVERE, "Exception during onShutdown()", ex);
    }
}
