package com.xiaohe.exception;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-02-04 22:36
 */
@SuppressWarnings("serial")
public class AlertException extends Exception {
    public static final AlertException INSTANCE = new AlertException();
    private AlertException() {}

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
