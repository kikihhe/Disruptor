package com.xiaohe.consumer.aware;

/**
 * @author : 小何
 * @Description : 所有消费者都可以使用的生命周期接口
 * @date : 2024-02-05 21:31
 */
public interface LifecycleAware {
    /**
     * 消费者是线程，执行run()方法后第一时间就执行这个方法
     */
    void onStart();

    /**
     * 消费者停止工作时执行此方法
     */
    void onShutdown();
}
