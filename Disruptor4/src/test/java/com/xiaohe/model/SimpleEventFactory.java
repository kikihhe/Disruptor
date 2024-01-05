package com.xiaohe.model;

import com.xiaohe.common.EventFactory;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-05 20:52
 */
public class SimpleEventFactory<T> implements EventFactory<Event<T>> {

    @Override
    public Event<T> newInstance() {
        return new Event<>();
    }
}
