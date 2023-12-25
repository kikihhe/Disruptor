package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 17:45
 */
public class Request<T> {
    private T data;

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
