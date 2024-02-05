package com.xiaohe.exception.handler;

/**
 * @author : 小何
 * @Description : 异常处理器
 * @date : 2024-02-05 21:47
 */
public interface ExceptionHandler<T> {
    /**
     * 消费出现异常时执行
     * @param t
     * @param sequence
     * @param event
     */
    void handleEventException(Throwable t, long sequence, T event);

    /**
     * 启动出现异常时执行
     * @param t
     */
    void handleOnStartException(Throwable t);

    /**
     * 关闭出现异常时执行
     * @param t
     */
    void handleOnShutdownException(Throwable t);
}
