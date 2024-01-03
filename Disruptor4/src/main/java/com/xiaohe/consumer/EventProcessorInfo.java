package com.xiaohe.consumer;

import com.xiaohe.common.Sequence;

import java.util.concurrent.Executor;

/**
 * 封装消费者信息，每一个 EventProcessorInfo都是一个消费者
 * @param <T>
 */
class EventProcessorInfo<T> implements ConsumerInfo {
    //消费者的事件处理器，该处理器中封装着用户定义的消费者handler
    private final EventProcessor eventprocessor;
    //用户定义的消费者handler
    private final EventHandler<? super T> handler;
    //消费者的序号屏障
    private final SequenceBarrier barrier;
    // 用来判断该消费者是不是最后的消费者
    // 这个就是和消费顺序有关系，有的消费者必须在某些消费者之后才能消费事件
    private boolean endOfChain = true;

    //构造方法
    EventProcessorInfo(
            final EventProcessor eventprocessor, final EventHandler<? super T> handler, final SequenceBarrier barrier) {
        this.eventprocessor = eventprocessor;
        this.handler = handler;
        this.barrier = barrier;
    }


    public EventProcessor getEventProcessor() {
        return eventprocessor;
    }

    @Override
    public Sequence[] getSequences() {
        return new Sequence[]{eventprocessor.getSequence()};
    }

    public EventHandler<? super T> getHandler() {
        return handler;
    }

    @Override
    public SequenceBarrier getBarrier() {
        return barrier;
    }

    @Override
    public boolean isEndOfChain() {
        return endOfChain;
    }

    @Override
    public void start(final Executor executor) {
        executor.execute(eventprocessor);
    }

    @Override
    public void halt() {
        eventprocessor.halt();
    }

    @Override
    public void markAsUsedInBarrier() {
        endOfChain = false;
    }

    @Override
    public boolean isRunning() {
        return eventprocessor.isRunning();
    }
}