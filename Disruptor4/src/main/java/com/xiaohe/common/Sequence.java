package com.xiaohe.common;

import com.xiaohe.util.Util;
import sun.misc.Unsafe;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-01 13:39
 */
class LhsPadding {
    //在父类填充空字节
    protected long p1, p2, p3, p4, p5, p6, p7;
}
// 父类中有 56 个无效字节，加上 value 自己的8个字节，一共64个字节，所以不管怎么样，value都会独占缓存行
class Value extends LhsPadding {

    protected volatile long value;
}
// 子类中有 56 个无效字节，加上 value 自己的8个字节，一共64个字节，所以不管怎么样，value都会独占缓存行
class RhsPadding extends Value {
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

    public long get()
    {
        return value;
    }

    public void set(final long value) {
        // 不保证立即可见，有几纳秒的延迟
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, value);
    }
    public void setVolatile(final long value) {
        // 保证立即可见，速度有点慢
        UNSAFE.putLongVolatile(this, VALUE_OFFSET, value);
    }

    public boolean compareAndSet(final long expectedValue, final long newValue) {
        return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, expectedValue, newValue);
    }

    public long incrementAndGet() {
        return addAndGet(1L);
    }

    public long addAndGet(final long increment) {
        long currentValue = 0;
        long newValue = 0;
        do {
            currentValue = get();
            newValue = currentValue + increment;
        } while (!compareAndSet(currentValue, newValue));
        return newValue;
    }



    @Override
    public String toString()
    {
        return Long.toString(get());
    }
}
