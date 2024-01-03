package com.xiaohe.consumer;

public interface LifecycleAware {

    void onStart();


    void onShutdown();
}