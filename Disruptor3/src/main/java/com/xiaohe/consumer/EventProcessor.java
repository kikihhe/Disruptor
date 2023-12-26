package com.xiaohe.consumer;

import com.xiaohe.Sequence;

/**
 * @author : 小何
 * @Description : 事件处理器，有两种：并行批处理器、并发处理器
 * @date : 2023-12-26 20:57
 */
public interface EventProcessor extends Runnable {
    /**
     * 获取消费者进度
     * @return
     */
    public Sequence getSequence();
}
