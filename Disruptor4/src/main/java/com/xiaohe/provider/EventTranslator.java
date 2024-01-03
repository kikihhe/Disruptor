package com.xiaohe.provider;

/**
 *
 * @param <T>
 */
public interface EventTranslator<T> {
    void translateTo(T event, long sequence);
}