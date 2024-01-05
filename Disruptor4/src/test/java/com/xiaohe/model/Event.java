package com.xiaohe.model;

public class Event<T> {

    private T data;

    public T getData() {
        return data;
    }

    public Event<T> setData(T data) {
        this.data = data;
        return this;
    }
}
