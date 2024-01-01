package com.xiaohe.util;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.EventProcessor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-31 20:10
 */
public class Util {
    public static int ceilingNextPowerOfTwo(final int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
    }

    /**
     * 寻找一组 Sequence中最小的那一个序号
     * @param sequences
     * @return
     */
    public static long getMinimumSequence(final Sequence[] sequences) {
        return getMinimumSequence(sequences, Long.MAX_VALUE);
    }

    /**
     * 得到消费者序号中比minimum还小的进度
     * @param sequences
     * @param minimum
     * @return
     */
    public static long getMinimumSequence(final Sequence[] sequences, long minimum) {
        for (int i = 0; i < sequences.length; i++) {
            long value = sequences[i].get();
            minimum = Math.min(minimum, value);
        }
        return minimum;
    }

    /**
     *  拿出一组EventProcessor中的一组Sequence
     * @param processors
     * @return
     */
    public static Sequence[] getSequenceFor(final EventProcessor... processors) {
        Sequence[] sequences = new Sequence[processors.length];
        for (int i = 0; i < sequences.length; i++) {
            sequences[i] = processors[i].getSequence();
        }
        return sequences;
    }


    private static final Unsafe THE_UNSAFE;
    static {
        try {
            final PrivilegedExceptionAction<Unsafe> action = new PrivilegedExceptionAction<Unsafe>() {
                @Override
                public Unsafe run() throws Exception {
                    Field thrUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    thrUnsafe.setAccessible(true);
                    return (Unsafe) thrUnsafe.get(null);
                }
            };
            THE_UNSAFE = AccessController.doPrivileged(action);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load unsafe", e);
        }
    }

    public static Unsafe getUnsafe() {
        return THE_UNSAFE;
    }
    public static int log2(int i) {
        int r = 0;
        while ((i >>= 1) != 0) {
            ++r;
        }
        return r;
    }
}
