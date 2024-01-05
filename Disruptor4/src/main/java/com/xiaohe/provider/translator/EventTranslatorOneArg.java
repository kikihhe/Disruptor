package com.xiaohe.provider.translator;

/**
 * @author : 小何
 * @Description : 将发布的数据放到 Event对象中，也就是将 T，装入 Event<T> 中
 * @date : 2024-01-01 15:01
 */
public interface EventTranslatorOneArg<T, A> {
    void translateTo(T event, long sequence, A arg0);
}
