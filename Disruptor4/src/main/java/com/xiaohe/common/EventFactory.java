package com.xiaohe.common;

/**
 * @author : 小何
 * @Description : 生成事件的工厂
 * @date : 2024-01-01 14:56
 */
public interface EventFactory<T> {
    T newInstance();
}
