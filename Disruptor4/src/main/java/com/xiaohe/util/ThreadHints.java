package com.xiaohe.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

public final class ThreadHints {
    private static final MethodHandle ON_SPIN_WAIT_METHOD_HANDLE;

    static {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();

        MethodHandle methodHandle = null;
        try {
            methodHandle = lookup.findStatic(Thread.class, "onSpinWait", methodType(void.class));
        } catch (final Exception ignore) {
        }

        ON_SPIN_WAIT_METHOD_HANDLE = methodHandle;
    }

    private ThreadHints() {
    }


    public static void onSpinWait() {
        if (null != ON_SPIN_WAIT_METHOD_HANDLE) {
            try {
                ON_SPIN_WAIT_METHOD_HANDLE.invokeExact();
            } catch (final Throwable ignore) {
            }
        }
    }
}