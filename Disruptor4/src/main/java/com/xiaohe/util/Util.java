package com.xiaohe.util;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.EventProcessor;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

public final class Util {
    public static int ceilingNextPowerOfTwo(final int x) {
        return 1 << (32 - Integer.numberOfLeadingZeros(x - 1));
    }


    public static long getMinimumSequence(final Sequence[] sequences) {
        return getMinimumSequence(sequences, Long.MAX_VALUE);
    }


    public static long getMinimumSequence(final Sequence[] sequences, long minimum) {
        for (int i = 0, n = sequences.length; i < n; i++) {
            long value = sequences[i].get();
            minimum = Math.min(minimum, value);
        }

        return minimum;
    }

    /**
     * 根据消费者，拿到这些消费者的消费进度
     * @param processors
     * @return
     */
    public static Sequence[] getSequencesFor(final EventProcessor... processors) {
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
                public Unsafe run() throws Exception {
                    Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                    theUnsafe.setAccessible(true);
                    return (Unsafe) theUnsafe.get(null);
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