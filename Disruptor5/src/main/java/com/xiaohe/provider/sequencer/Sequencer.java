package com.xiaohe.provider.sequencer;

import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.provider.Cursored;

/**
 * @author : 小何
 * @Description : 序号生成器顶级接口
 * @date : 2024-02-05 22:06
 */
public interface Sequencer extends Sequenced, Cursored {
    /**
     * 将生产者序号指定为 sequence
     * @param sequence
     */
    public void claim(long sequence);

    /**
     * 判断进度是否可用
     * @param sequence
     * @return
     */
    public boolean isAvailable(long sequence);

    /**
     * Sequencer 内部要存放所有消费者的进度，使用addGatingSequences可以将这些进度添加到数组中
     * 把新这些添加进来的消费者的消费序号添加到 gatingSequences 数组中
     * @param addGatingSequences
     */
    public void addGatingSequences(Sequence... addGatingSequences);

    /**
     * 从 gatingSequences 数组中移除不必再关系的消费者的消费序号
     * @param sequence
     * @return
     */
    boolean removeGatingSequence(Sequence sequence);

    /**
     * 给一个消费者创建序号屏障，这个消费者可能要依赖其他消费者的进度
     * @param sequencesToTrack 被依赖的消费者们的进度
     * @return
     */
    public SequenceBarrier newBarrier(Sequence... sequencesToTrack);

    /**
     * 得到所有消费者和当前生产者序号中最小的那个
     * @return
     */
    public long getMinimumSequence();

    /**
     * 得到已经发布的最大的生产者序号
     * 也就是 RingBuffer 中从 nextSequence 到 availableSequence 之间可用的最大序号
     * @param nextSequence
     * @param availableSequence
     * @return
     */
    public long getHighestPublishedSequence(long nextSequence, long availableSequence);
}
