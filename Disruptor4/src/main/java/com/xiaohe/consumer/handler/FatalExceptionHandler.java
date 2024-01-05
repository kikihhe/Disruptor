package com.xiaohe.consumer.handler;

import java.util.logging.Level;
import java.util.logging.Logger;
public final class FatalExceptionHandler implements ExceptionHandler<Object> {
    private static final Logger LOGGER = Logger.getLogger(FatalExceptionHandler.class.getName());
    private final Logger logger;

    public FatalExceptionHandler() {
        this.logger = LOGGER;
    }

    public FatalExceptionHandler(final Logger logger) {
        this.logger = logger;
    }

    /**
     * 在处理异常的过程中会再次抛出异常，消费者会直接被迫终止
     */
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
