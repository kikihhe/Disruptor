package com.xiaohe.consumer.aware;

/**
 * 批量消费者、并发消费者都可以使用。
 */
public interface LifecycleAware {

    /**
     * 消费者是个线程，执行run方法后第一时间执行这个 onStart
     */
    void onStart();


    /**
     * 消费者停止工作时执行此方法
     */
    void onShutdown();
}