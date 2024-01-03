package com.xiaohe.common;

import com.xiaohe.util.Util;

import java.util.Arrays;

/**
 * @author : 小何
 * @Description : 顺序消费时，一个消费者依赖的一组消费者。这个类只支持查看最慢的消费进度
 * @date : 2024-01-01 13:51
 */
public class FixedSequenceGroup extends Sequence {
    private final Sequence[] sequences;

    public FixedSequenceGroup(Sequence[] sequences) {
        this.sequences = Arrays.copyOf(sequences, sequences.length);
    }

    /**
     * 得到被依赖的消费者中消费最慢的那个进度
     * @return
     */
    @Override
    public long get() {
        return Util.getMinimumSequence(sequences);
    }

    /**
     * 不支持批量操作
     * @param value
     */
    @Override
    public void set(long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long incrementAndGet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return Arrays.toString(sequences);
    }

    @Override
    public long addAndGet(long increment) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSet(long expectedValue, long newValue) {
        throw new UnsupportedOperationException();
    }
}
