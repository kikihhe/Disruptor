package com.xiaohe.provider;

/**
 * 批量传输数据
 * @param <T>
 */
public interface EventTranslatorVararg<T> {

    void translateTo(T event, long sequence, Object... args);
}