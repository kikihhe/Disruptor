package com.xiaohe.consumer;

import com.xiaohe.common.Sequence;

/**
 * @author : 小何
 * @Description : 消费者的顶级接口，有两种实现类:并行消费，并发消费
 * @date : 2024-02-06 12:18
 */
public interface EventProcessor extends Runnable {
    /**
     * 获取此消费者的进度序号
     * @return
     */
    public Sequence getSequence();

    /**
     * 终止该消费者
     */
    public void halt();

    public boolean isRunning();
}
