package com.xiaohe.consumer;

import com.xiaohe.Sequence;
import com.xiaohe.wait.WaitStrategy;

/**
 * @author : 小何
 * @Description : 消费者的序号屏障。内含生产者进度和阻塞策略，可以根据进度来判断是否需要阻塞。
 * @date : 2023-12-26 21:32
 */
public class ProcessingSequenceBarrier {
    private final WaitStrategy waitStrategy;

    /**
     * 生产者进度
     */
    private final Sequence cursorSequence;

    public ProcessingSequenceBarrier(WaitStrategy waitStrategy, Sequence cursorSequence) {
        this.waitStrategy = waitStrategy;
        this.cursorSequence = cursorSequence;
    }

    /**
     * 将消费者进度传进来
     * @param sequence 消费者希望消费的下标
     * @return
     */
    public long waitFor(final long sequence) {
        long availableSequence = waitStrategy.waitFor(sequence, cursorSequence);
        // 一般不会走这里
        if (availableSequence < sequence) {
            return availableSequence;
        }
        return availableSequence;
    }
}
