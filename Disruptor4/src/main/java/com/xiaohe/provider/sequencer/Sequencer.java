package com.xiaohe.provider.sequencer;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.provider.Cursored;
import com.xiaohe.provider.Sequenced;

/**
 * @author : 小何
 * @Description : 序号生成器接口
 * @date : 2024-01-01 14:09
 */
public interface Sequencer extends Cursored, Sequenced {
    long INITIAL_CURSOR_VALUE = -1L;

    /**
     * 将生产者序号指定为 sequence
     * @param sequence
     */
    void claim(long sequence);

    /**
     * 判断一个序号是否可用
     * @param sequence
     * @return
     */
    boolean isAvailable(long sequence);

    /**
     * 把新添加进来的消费者的消费序号添加到 gatingSequences 数组中
     * @param gatingSequences
     */
    void addGatingSequences(Sequence... gatingSequences);

    /**
     * 从 gatingSequences 数组中移除不必再关系的消费者的消费序号
     * @param sequence
     * @return
     */
    boolean removeGatingSequence(Sequence sequence);

    /**
     * 为消费者创建序号屏障
     * @param sequencesToTrack 此消费者依赖的所有其他消费者
     * @return
     */
    SequenceBarrier newBarrier(Sequence...sequencesToTrack);

    /**
     * 得到所有消费者序号 和 当前生产者序号 中最小的那个序号
     * @return
     */
    long getMinimumSequence();

    /**
     * 得到已经发布的最大的生产者序号
     * 也就是RingBuffer中从 nextSequence 到 availableSequence 之间可用的最大序号
     * @param nextSequence
     * @param availableSequence
     * @return
     */
    long getHighestPublishedSequence(long nextSequence, long availableSequence);

}
