package com.xiaohe.provider;

import com.xiaohe.Sequence;
import com.xiaohe.wait.WaitStrategy;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author : 小何
 * @Description : 序号生成器的抽象父类
 * 子类有单生产者序号生成器、多生产者序号生成器..
 * @date : 2023-12-31 21:11
 */
public abstract class AbstractSequencer {
    /**
     * 原子更新器，拿到gatingSequences可以对齐进行原子更新
     */
    private static final AtomicReferenceFieldUpdater<AbstractSequencer, Sequence[]> SEQUENCE_UPDATE =
            AtomicReferenceFieldUpdater.newUpdater(AbstractSequencer.class, Sequence[].class, "gatingSequences");

    /**
     * 此序号生成器拥有的所有的消费者的序号
     */
    protected volatile Sequence[] gatingSequences = new Sequence[0];
    /**
     * 环形数组的大小
     */
    protected final int bufferSize;

    /**
     * 等待策略
     * 之所以把等待策略放在这里，是为了在序号生成器中创建序号屏障，阻塞策略可以直接传递给序号屏障
     */
    protected final WaitStrategy waitStrategy;
    /**
     * 生产者的进度
     */
    protected final Sequence cursor = new Sequence(Sequence.INITIAL_VALUE);

    public AbstractSequencer(int bufferSize, WaitStrategy waitStrategy) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must be less than 1");
        }
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("buffer must be a power of 2");
        }
        this.bufferSize = bufferSize;
        this.waitStrategy = waitStrategy;
    }

    public abstract long next(int n);
}
