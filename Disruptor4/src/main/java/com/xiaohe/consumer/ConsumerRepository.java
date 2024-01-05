package com.xiaohe.consumer;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.batch.EventHandler;
import com.xiaohe.consumer.multi.WorkerPool;
import com.xiaohe.consumer.multi.WorkerPoolInfo;


import java.util.*;

/**
 * @author : 小何
 * @Description : 封装消费者，将 handler 和 EventProcessor 做一个对应。把所有消费者信息封装到一起，处理的时候一起处理
 * @date : 2024-01-03 21:45
 */
public class ConsumerRepository<T> implements Iterable<ConsumerInfo> {

    private final Map<EventHandler<?>, EventProcessorInfo<T>> eventProcessorInfoByEventHandler = new IdentityHashMap<>();
    private final Map<Sequence, ConsumerInfo> eventProcessorInfoBySequence = new IdentityHashMap<>();
    private final Collection<ConsumerInfo> consumerInfos = new ArrayList<>();
    //迭代器方法
    @Override
    public Iterator<ConsumerInfo> iterator() {
        return consumerInfos.iterator();
    }


    public void add(final EventProcessor eventprocessor,
                    final EventHandler<? super T> handler,
                    final SequenceBarrier barrier) {

        final EventProcessorInfo<T> consumerInfo = new EventProcessorInfo<>(eventprocessor, handler, barrier);
        eventProcessorInfoByEventHandler.put(handler, consumerInfo);
        eventProcessorInfoBySequence.put(eventprocessor.getSequence(), consumerInfo);
        consumerInfos.add(consumerInfo);
    }


    public void add(final EventProcessor processor) {
        final EventProcessorInfo<T> consumerInfo = new EventProcessorInfo<>(processor, null, null);
        eventProcessorInfoBySequence.put(processor.getSequence(), consumerInfo);
        consumerInfos.add(consumerInfo);
    }

    public void add(final WorkerPool<T> workerPool, final SequenceBarrier sequenceBarrier) {   //把workerPool封装到WorkerPoolInfo中
        final WorkerPoolInfo<T> workerPoolInfo = new WorkerPoolInfo<>(workerPool, sequenceBarrier);
        //添加到集合中
        consumerInfos.add(workerPoolInfo);
        //遍历workerPool对象管理的每一个消费者的Sequence，而Sequence封装着每一个消费者的消费进度
        for (Sequence sequence : workerPool.getWorkerSequences()) {   //在这里把消费进度和对应的消费者信息添加到Map中
            eventProcessorInfoBySequence.put(sequence, workerPoolInfo);
        }
    }

    /**
     * 获取消费的最慢的那个消费者的进度
     * @param includeStopped
     * @return
     */
    public Sequence[] getLastSequenceInChain(boolean includeStopped) {
        List<Sequence> lastSequence = new ArrayList<>();
        for (ConsumerInfo consumerInfo : consumerInfos) {
            if ((includeStopped || consumerInfo.isRunning()) && consumerInfo.isEndOfChain()) {
                final Sequence[] sequences = consumerInfo.getSequences();
                Collections.addAll(lastSequence, sequences);
            }
        }
        return lastSequence.toArray(new Sequence[lastSequence.size()]);
    }

    public EventProcessor getEventProcessorFor(final EventHandler<T> handler) {
        final EventProcessorInfo<T> eventprocessorInfo = getEventProcessorInfo(handler);
        if (eventprocessorInfo == null) {
            throw new IllegalArgumentException("The event handler " + handler + " is not processing events.");
        }
        return eventprocessorInfo.getEventProcessor();
    }


    public Sequence getSequenceFor(final EventHandler<T> handler) {
        return getEventProcessorFor(handler).getSequence();
    }

    public void unMarkEventProcessorsAsEndOfChain(final Sequence... barrierEventProcessors) {
        for (Sequence barrierEventProcessor : barrierEventProcessors) {
            getEventProcessorInfo(barrierEventProcessor).markAsUsedInBarrier();
        }
    }

    /**
     * 根据handler获得消费者的序号屏障. 每一个消费者都有自己专属的序号屏障
     * @param handler
     * @return
     */
    public SequenceBarrier getBarrierFor(final EventHandler<T> handler) {
        final ConsumerInfo consumerInfo = getEventProcessorInfo(handler);
        return consumerInfo != null ? consumerInfo.getBarrier() : null;
    }

    /**
     * 通过handler获得消费者的EventProcessorInfo
     * @param handler
     * @return
     */
    private EventProcessorInfo<T> getEventProcessorInfo(final EventHandler<T> handler) {
        return eventProcessorInfoByEventHandler.get(handler);
    }



    /**
     * 根据消费者的消费进度获取对应的消费者
     * @param barrierEventProcessor
     * @return
     */
    private ConsumerInfo getEventProcessorInfo(final Sequence barrierEventProcessor) {
        return eventProcessorInfoBySequence.get(barrierEventProcessor);
    }
}
