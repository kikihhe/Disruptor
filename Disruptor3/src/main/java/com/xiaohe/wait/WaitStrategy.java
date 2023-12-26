package com.xiaohe.wait;

import com.xiaohe.Sequence;

public interface WaitStrategy {
    public long waitFor(final long sequence, Sequence cursor);
}
