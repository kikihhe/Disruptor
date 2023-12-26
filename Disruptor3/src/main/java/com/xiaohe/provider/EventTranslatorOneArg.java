package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description : 传输器
 * 将生产者发布的真正的数据传输到对应的对象中。
 * T : 环形数组中的元素，一般是 Request<A>
 * A : 我们真正产生的数据，Request中的数据
 * @date : 2023-12-25 17:35
 */
public interface EventTranslatorOneArg<T, A> {
    /**
     * 将数据arg0塞到event中
     * @param event
     * @param sequence
     * @param arg0
     */
    void translateTo(T event, long sequence, A arg0);
}
