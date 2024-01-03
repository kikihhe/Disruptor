package com.xiaohe.provider;

import com.xiaohe.common.Sequence;
import com.xiaohe.common.SequenceGroups;
import com.xiaohe.consumer.ProcessingSequenceBarrier;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.util.Util;
import com.xiaohe.util.wait.WaitStrategy;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author : 小何
 * @Description : 序号生成器，子类有单机的序号生成器、并发的序号生成器
 * @date : 2024-01-01 16:28
 */
public abstract class AbstractSequencer implements Sequencer {

    /**
     * 环形数组容量
     */
    protected final int bufferSize;

    /**
     * 等待策略
     */
    protected final WaitStrategy waitStrategy;

    /**
     * 生产者当前进度
     */
    protected final Sequence cursor = new Sequence(Sequence.INITIAL_VALUE);


    /**
     * 所有消费者的进度。虽然 Sequence 不会变，但是其内部的 value 变量是一直在改变的。
     */
    protected volatile Sequence[] gatingSequences = new Sequence[0];

    /**
     * 线程安全的操作 gatingSequences 数组的工具
     */
    private static final AtomicReferenceFieldUpdater<AbstractSequencer, Sequence[]> SEQUENCE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(AbstractSequencer.class, Sequence[].class, "gatingSequences");


    public AbstractSequencer(int bufferSize, WaitStrategy waitStrategy) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize must not be less than 1");
        }
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("bufferSize must be a power of 2");
        }
        this.bufferSize = bufferSize;
        this.waitStrategy = waitStrategy;
    }


    @Override
    public SequenceBarrier newBarrier(Sequence... sequencesToTrack) {
        return new ProcessingSequenceBarrier(this, waitStrategy, cursor, sequencesToTrack);
    }

    @Override
    public long getCursor() {
        return cursor.get();
    }

    /**
     * 获取环形数组的有效容量
     * @return
     */
    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * 添加序号到 gatingSequences
     * @param gatingSequences
     */
    @Override
    public final void addGatingSequences(Sequence... gatingSequences) {
        SequenceGroups.addSequences(this, SEQUENCE_UPDATER, this, gatingSequences);
    }

    /**
     * 从 gatingSequences 中删除指定的序号
     * @param sequence
     * @return
     */
    @Override
    public boolean removeGatingSequence(Sequence sequence) {
        return SequenceGroups.removeSequence(this, SEQUENCE_UPDATER, sequence);
    }

    /**
     * 得到所有 消费者序号 和 当前生产者序号 中最小的那个序号
     * @return
     */
    @Override
    public long getMinimumSequence() {
        return Util.getMinimumSequence(gatingSequences, cursor.get());
    }



    @Override
    public String toString() {
        return "AbstractSequencer{" +
                "waitStrategy=" + waitStrategy +
                ", cursor=" + cursor +
                ", gatingSequences=" + Arrays.toString(gatingSequences) +
                '}';
    }

}
