package com.xiaohe;

import com.xiaohe.util.Util;
import sun.misc.Unsafe;

/**
 * @author : 小何
 * @Description : 进度序号
 * @date : 2023-12-25 16:50
 */
class LhsPadding {
    protected long p1, p2, p3, p4, p5, p6, p7;
}

class Value extends LhsPadding {
    protected volatile long value;
}

class RhsPadding extends Value {
    protected long p9, p10, p11, p12, p13, p14, p15;
}

public class Sequence extends RhsPadding {
    /**
     * Sequence默认值
     */
    public static final long INITIAL_VALUE = -1;

    private static final Unsafe UNSAFE;

    private static final long VALUE_OFFSET;

    static {
        UNSAFE = Util.getUnsafe();
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(Value.class.getDeclaredField("value"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Sequence() {
        this(INITIAL_VALUE);
    }

    public Sequence(final long value) {
        // 使用 Unsafe.putOrderedLong 更新不会立即被其他线程看到，会延迟几纳秒
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, value);
    }


    public void setVolatile(final long value) {
        // 使用 Unsafe.putLongVolatile 会立即让其他线程感知到
        // 原理是直接写入主存，所以有些慢，不常用这个方法，只有消费者进度比较慢时，生产者使用它更新进度，减小由于内存不可见带来的延迟影响
        UNSAFE.putLongVolatile(this, VALUE_OFFSET, value);
    }

    /**
     * 多生产者模式下使用
     *
     * @param expectedValue
     * @param newValue
     */
    public boolean compareAndSet(final long expectedValue, final long newValue) {
        return UNSAFE.compareAndSwapLong(this, VALUE_OFFSET, expectedValue, newValue);
    }

    public long incrementAndGet() {
        return addAndGet(1L);
    }

    /**
     * 增加并且获取，使用CAS保证并发情况下的安全。
     * 每次CAS都重新获取当前value, 然后使用 CAS 将 value 改为 value+increment
     *
     * @param increment
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

    public void set(final long value) {
        // 使用 Unsafe.putOrderedLong 更新不会立即被其他线程看到，会延迟几纳秒
        UNSAFE.putOrderedLong(this, VALUE_OFFSET, value);
    }
}
