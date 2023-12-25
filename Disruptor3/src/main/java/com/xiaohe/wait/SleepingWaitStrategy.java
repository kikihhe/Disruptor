package com.xiaohe.wait;

import com.xiaohe.Sequence;

import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 16:40
 */
public class SleepingWaitStrategy implements WaitStrategy{

    private static final int DEFAULT_RETRIES = 200;
    public static final long DEFAULT_SLEEP = 100;

    private int retries;

    private final long sleepTimeNs;

    public SleepingWaitStrategy() {
        this(DEFAULT_RETRIES, DEFAULT_SLEEP);
    }

    public SleepingWaitStrategy(int retries, long sleepTimeNs) {
        this.retries = retries;
        this.sleepTimeNs = sleepTimeNs;
    }

    /**
     * 判断是否需要阻塞
     * @param a 生产者进度, 由于生产者进度是可变的
     * @param b 消费者进度
     */
    @Override
    public void waitFor(Sequence a, long b) {
        int counter = retries;
        while (a.getValue() < b) {
            counter = applyWaitMethod(counter);
        }
    }

    private int applyWaitMethod(int counter) {
        if (counter > 100) {
            counter--;
        } else if (counter > 100) {
            counter--;
            Thread.yield();
        } else {
            LockSupport.parkNanos(sleepTimeNs);
        }
        return counter;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }
}
