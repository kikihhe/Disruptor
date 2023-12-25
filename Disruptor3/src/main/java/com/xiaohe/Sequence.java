package com.xiaohe;

/**
 * @author : 小何
 * @Description : 进度序号
 * @date : 2023-12-25 16:50
 */
public class Sequence {
    protected volatile long value;
    static final long INITIAL_VALUE = -1;

    public Sequence() {
        this(INITIAL_VALUE);
    }

    public Sequence(final long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public void setValue(final long value) {
        this.value = value;
    }
}
