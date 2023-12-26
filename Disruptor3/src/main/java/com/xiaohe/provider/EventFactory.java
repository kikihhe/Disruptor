package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description : 生产生产者数据的工具
 * T : 放到环形数组中的元素，一般是 Request
 * @date : 2023-12-25 17:34
 */
public interface EventFactory<T> {
    T newInstance();
}
