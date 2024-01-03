package com.xiaohe.consumer;

import com.xiaohe.common.Sequence;

/**
 * @author : 小何
 * @Description : 包装消费者，并行消费者、并发消费者都要实现它。每一个 EventProcessor 都是一个消费者
 * @date : 2024-01-01 14:57
 */
public interface EventProcessor extends Runnable {
    Sequence getSequence();

    /**
     * 终止线程
     */
    void halt();

    /**
     * 是否正在运行
     * @return
     */
    boolean isRunning();

}
