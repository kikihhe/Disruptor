package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 17:45
 */
public class SimpleEventFactory<T> implements EventFactory<Request<T>> {
    @Override
    public Request<T> newInstance() {
        return new Request<>();
    }
}
