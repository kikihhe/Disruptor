package com.xiaohe;

import com.xiaohe.util.Util;

import java.util.Arrays;

/**
 * @author : 小何
 * @Description : 顺序消费，消费者依赖的其他消费者的消费进度
 * @date : 2024-01-01 13:15
 */
public class FixedSequenceGroup extends Sequence {
    private final Sequence[] sequences;

    public FixedSequenceGroup(Sequence[] sequences) {
        this.sequences = Arrays.copyOf(sequences, sequences.length);
    }

    /**
     * 返回所有被依赖的消费者的最小消费进度
     * @return
     */
    public long get() {
        return Util.getMinimumSequence(sequences);
    }
}
