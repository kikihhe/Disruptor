package com.xiaohe.provider;

public interface EventSink<E> {

    void publishEvent(EventTranslator<E> translator);

    <A> void publishEvent(EventTranslatorOneArg<E, A> translator, A arg0);

}