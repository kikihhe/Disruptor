package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-26 21:09
 */
public interface DataProvider<T> {
    public T get(long sequence);
}
