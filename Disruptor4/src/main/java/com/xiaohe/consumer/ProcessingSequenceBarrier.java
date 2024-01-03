package com.xiaohe.consumer;

import com.xiaohe.common.FixedSequenceGroup;
import com.xiaohe.common.Sequence;
import com.xiaohe.exception.AlertException;
import com.xiaohe.provider.Sequencer;
import com.xiaohe.util.wait.WaitStrategy;

import java.util.concurrent.TimeoutException;

/**
 * @author : 小何
 * @Description : 序号屏蔽器
 * 序号屏蔽器拥有生产者序号，以及此消费者依赖的序号
 * @date : 2024-01-01 15:39
 */
public class ProcessingSequenceBarrier implements SequenceBarrier {

    private final WaitStrategy waitStrategy;
    /**
     * 此消费者依赖的序号进度
     * 如果不是顺序消费，这个值就是生产者进度
     * 如果是顺序消费，这个值就是被依赖的消费者们的进度
     */
    private final Sequence dependentSequence;
    /**
     * 生产者序号
     */
    private final Sequence cursorSequence;
    /**
     * 消费者是否终止
     */
    private volatile boolean alerted = false;

    /**
     * 序号生成器，给生产者分配序号的
     */
    private final Sequencer sequencer;


    public ProcessingSequenceBarrier(final Sequencer sequencer,
                              final WaitStrategy waitStrategy,
                              final Sequence cursorSequence,
                              final Sequence[] dependentSequences) {
        this.sequencer = sequencer;
        this.waitStrategy = waitStrategy;
        this.cursorSequence = cursorSequence;
        if (dependentSequences.length == 0) {
            dependentSequence = cursorSequence;
        } else {
            dependentSequence = new FixedSequenceGroup(dependentSequences);
        }
    }

    /**
     * 消费者拿着自己已经消费的序号来看，sequence 后哪些序号可以进行消费了，也就是拿着自己的序号来要新序号
     * @param sequence 消费者现在的序号，因为想要申请新序号就要拿老序号来对比。
     * @return
     * @throws AlertException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    @Override
    public long waitFor(long sequence) throws AlertException, InterruptedException, TimeoutException {
        checkAlert();
        // 使用阻塞策略得到生产者分配后的序号
        long availableSequence = waitStrategy.waitFor(sequence, cursorSequence, dependentSequence, this);
        if (availableSequence < sequence) {
            return availableSequence;
        }
        // sequence 到 availableSequence 只是生产者已经分配的序号，但是生产者可能没来得及给这些序号放数据，所以现在看看到底哪些能消费
        // 返回一个具体的值
        // sequencer是分配序号的，它肯定知道哪里有数据。
        return sequencer.getHighestPublishedSequence(sequence, availableSequence);
    }

    /**
     * 根据此消费者依赖的是谁，来给出它的进度。
     * @return
     */
    @Override
    public long getCursor() {
        return dependentSequence.get();
    }

    @Override
    public boolean isAlerted() {
        return alerted;
    }

    @Override
    public void alert() {
        // 该方法一旦被调用，就意味着消费者要被终止了，所以把alerted设置为true即可
        alerted = true;
        // 唤醒所有等待的线程
        waitStrategy.signalAllWhenBlocking();
    }

    @Override
    public void clearAlert() {
        alerted = false;
    }

    @Override
    public void checkAlert() throws AlertException {
        if (alerted) {
            throw AlertException.INSTANCE;
        }
    }
}
