package com.xiaohe.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * @author : 小何
 * @Description : 工具类，主要有两个作用:
 *                1. 获取 Unsafe
 *                2. 给一组消费进度，找出其中最慢的消费进度
 * @date : 2024-02-04 22:41
 */
public class Util {
    private static final Unsafe UNSAFE;
    static {
        try {
            final PrivilegedExceptionAction<Unsafe> action = () -> {
                Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                return (Unsafe) theUnsafe.get(null);
            };
            UNSAFE = AccessController.doPrivileged(action);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load unsafe", e);
        }
    }
    public static Unsafe getUnsafe() {
        return UNSAFE;
    }
}
