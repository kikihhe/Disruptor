package com.xiaohe.wait;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.ProcessingSequenceBarrier;

public interface WaitStrategy {
    public long waitFor(final long sequence, Sequence cursorSequence, Sequence dependentSequence, ProcessingSequenceBarrier cursor);
}
