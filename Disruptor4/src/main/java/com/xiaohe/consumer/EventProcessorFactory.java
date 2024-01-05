package com.xiaohe.consumer;

import com.xiaohe.RingBuffer;
import com.xiaohe.common.Sequence;

public interface EventProcessorFactory<T> {

    EventProcessor createEventProcessor(RingBuffer<T> ringBuffer, Sequence[] barrierSequences);
}