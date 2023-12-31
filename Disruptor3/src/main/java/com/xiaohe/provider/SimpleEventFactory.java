package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description : 生产数据的工厂
 * T : 具体的数据，比如 Integer、User...
 * @date : 2023-12-25 17:45
 */
public class SimpleEventFactory<T> implements EventFactory<Request<T>> {
    @Override
    public Request<T> newInstance() {
        return new Request<>();
    }
}
