package com.xiaohe;

public interface DataProvider<T> {

    T get(long sequence);
}