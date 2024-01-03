package com.xiaohe.provider;

import com.xiaohe.common.Event;
import com.xiaohe.common.EventFactory;

/**
 * @author : 小何
 * @Description : 最简单的 Event 生成器
 * @date : 2024-01-01 15:03
 */
public class SimpleEventFactory<T> implements EventFactory<Event<T>> {
    @Override
    public Event<T> newInstance() {
        return new Event<>();
    }
}
