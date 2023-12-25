package com.xiaohe.strategy.wait.impl;

import com.xiaohe.strategy.wait.WaitStrategy;

import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 12:09
 */
public class SleepingWaitStrategy implements WaitStrategy {
    /**
     * 默认自旋次数
     */
    private static final int DEFAULT_RETRIES = 200;
    /**
     * 每次睡眠时间，单位纳秒
     */
    private static final int DEFAULT_SLEEP = 100;

    private int retries;

    private long sleepTimeNs;

    //无参构造方法
    public SleepingWaitStrategy() {
        this(DEFAULT_RETRIES, DEFAULT_SLEEP);
    }

    public SleepingWaitStrategy(int retries, long sleepTimeNs) {
        //自旋次数被赋值为200
        this.retries = retries;
        //睡眠时间被赋值为100
        this.sleepTimeNs = sleepTimeNs;
    }

    @Override
    public void waitFor() {
        if (retries > 100) {
            --retries;
        } else if (retries > 0) {
            --retries;
            Thread.yield();
        } else {
            LockSupport.parkNanos(sleepTimeNs);
        }
    }

    /**
     * 可用于重试次数的清零
     * @param retries
     */
    public void setRetries(int retries) {
        this.retries = retries;
    }
}
