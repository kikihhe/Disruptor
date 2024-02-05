package com.xiaohe.exception.handler;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-02-05 21:50
 */
public interface TimeoutHandler {
    /**
     * 执行时发生超时异常
     * @param sequence
     * @throws Exception
     */
    void onTimeout(long sequence) throws Exception;
}
