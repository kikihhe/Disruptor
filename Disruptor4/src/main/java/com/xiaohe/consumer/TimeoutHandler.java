package com.xiaohe.consumer;

public interface TimeoutHandler {
    void onTimeout(long sequence) throws Exception;
}