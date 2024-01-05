package com.xiaohe.provider;

import com.xiaohe.provider.translator.EventTranslatorOneArg;

public interface EventSink<E> {

    void publishEvent(EventTranslator<E> translator);

    <A> void publishEvent(EventTranslatorOneArg<E, A> translator, A arg0);

}