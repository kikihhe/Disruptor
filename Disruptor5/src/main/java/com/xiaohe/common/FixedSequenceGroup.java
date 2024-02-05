package com.xiaohe.common;

import com.xiaohe.util.Util;

import java.util.Arrays;

/**
 * @author : 小何
 * @Description : 顺序消费时，一个消费者依赖的那一组消费者。这个消费者组虽然继承了Sequence，但是get方法只能查看消费进度最慢的消费者
 * @date : 2024-02-05 20:46
 */
public class FixedSequenceGroup extends Sequence {
    private Sequence[] sequences;

    public FixedSequenceGroup(Sequence[] sequences) {
        this.sequences = Arrays.copyOf(sequences, sequences.length);
    }

    /**
     * 获得整个组中消费者的最慢进度
     * @return
     */
    @Override
    public long get() {
        return Util.getMinimumSequence(this.sequences);
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
