package com.xiaohe.consumer.batch;

import com.xiaohe.Disruptor;
import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.ConsumerRepository;
import com.xiaohe.consumer.EventProcessor;

import java.util.Arrays;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-05 18:55
 */
public class EventHandlerGroup<T> {
    private final Disruptor<T> disruptor;

    private final ConsumerRepository<T> consumerRepository;

    private final Sequence[] sequences;

    public EventHandlerGroup(final Disruptor<T> disruptor,
                             final ConsumerRepository<T> consumerRepository,
                             final Sequence[] sequences) {
        this.disruptor = disruptor;
        this.consumerRepository = consumerRepository;
        this.sequences = Arrays.copyOf(sequences, sequences.length);
    }

    /**
     * 将其他的 EventHandlerGroup 与这个 EventHandlerGroup 合并
     * @param otherHandlerGroup
     * @return
     */
    public EventHandlerGroup<T> add(final EventHandlerGroup<T> otherHandlerGroup) {
        final Sequence[] combinedSequences = new Sequence[this.sequences.length + otherHandlerGroup.sequences.length];
        System.arraycopy(this.sequences, 0, combinedSequences, 0, this.sequences.length);
        System.arraycopy(
                otherHandlerGroup.sequences, 0,
                combinedSequences, this.sequences.length, otherHandlerGroup.sequences.length);
        return new EventHandlerGroup<>(disruptor, consumerRepository, combinedSequences);
    }

    /**
     * 将多个 EventProcessor 添加到这个 EventHandlerGroup 中
     * @param processors
     * @return
     */
    public EventHandlerGroup<T> add(final EventProcessor... processors) {
        Sequence[] combinedSequences = new Sequence[sequences.length + processors.length];
        for (int i = 0; i < processors.length; i++) {
            consumerRepository.add(processors[i]);
            combinedSequences[i] = processors[i].getSequence();
        }
        System.arraycopy(sequences, 0, combinedSequences, processors.length, sequences.length);
        return new EventHandlerGroup<>(disruptor, consumerRepository, combinedSequences);
    }

    public final EventHandlerGroup<T> handleEventsWith(final EventHandler<? super T> ... handlers) {
        return disruptor.createEventProcessors(sequences, handlers);
    }
}
