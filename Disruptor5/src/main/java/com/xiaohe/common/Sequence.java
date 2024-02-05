package com.xiaohe.common;

import com.xiaohe.util.Util;
import sun.misc.Unsafe;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-02-05 20:30
 */
class LhsPadding {
    /**
     * 在父类填充的字节
     */
    protected long p1, p2, p3, p4, p5, p6, p7;
}
class Value extends LhsPadding {
    /**
     * 有效数据
     * 父类和字节中各有56个字节的填充，加上value，不管怎样value都可以独占缓存行
     */
    protected volatile long value;
}
class RhsPadding extends Value {
    /**
     * 在子类填充的字节
     */
    protected long p9, p10, p11, p12, p13, p14, p15;
}
public class Sequence extends RhsPadding {
    public static final long INITIAL_VALUE = -1L;
    private static final Unsafe UNSAFE;
    private static final long VALUE_OFFSET;
    static {
        UNSAFE = Util.getUnsafe();
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(Value.class.getDeclaredField("value"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Sequence() {
        this(INITIAL_VALUE);
    }
    public Sequence(final long initialValue) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, initialValue);
    }

    public void set(final long value) {
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, value);
    }
    public void setVolatile(final long value) {
        UNSAFE.putLongVolatile(this, VALUE_OFFSET, value);
    }

    public boolean compareAndSet(final long expectedValue, final long newValue) {
        return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, expectedValue, newValue);
    }
    public long incrementAndGet() {
        return addAndGet(1);
    }

    /**
     * 增加指定数量，并且返回值. 使用CAS保证一定成功
     * @param increment
     * @return
     */
    public long addAndGet(final long increment) {
        long currentValue;
        long newValue;
        do {
            currentValue = get();
            newValue = currentValue + increment;
        } while (!compareAndSet(currentValue, newValue));
        return newValue;
    }
    public long get() {
        return value;
    }
    @Override
    public String toString() {
        return Long.toString(get());
    }
}
