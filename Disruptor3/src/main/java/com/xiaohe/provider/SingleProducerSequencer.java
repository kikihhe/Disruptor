package com.xiaohe.provider;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.ProcessingSequenceBarrier;
import com.xiaohe.wait.WaitStrategy;

import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description : 生产者的序号屏障，内置生产者、消费者的进度
 * 1. 根据消费者的进度申请生产者可用的序号
 * 2. 生产者生产数据后，使用 publish方法将生产者的序号更改，
 * @date : 2023-12-26 21:50
 */
public class SingleProducerSequencer {
    /**
     * 环形数组的容量
     */
    protected final int bufferSize;

    /**
     * 消费者的进度
     */
    protected Sequence consumer;

    /**
     * 生产者的当前进度
     */
    protected final Sequence cursor = new Sequence(Sequence.INITIAL_VALUE);

    /**
     * 等待策略
     * 之所以把等待策略放在这里，是为了在序号生成器中创建序号屏障，阻塞策略可以直接传递给序号屏障
     */
    protected final WaitStrategy waitStrategy;

    /**
     * 当前分配的可用序号
     */
    long nextValue = Sequence.INITIAL_VALUE;

    public SingleProducerSequencer(int bufferSize, WaitStrategy waitStrategy) {
        this.bufferSize = bufferSize;
        this.waitStrategy = waitStrategy;
    }

    /**
     * 获取一个可用序号
     * @return
     */
    public long next() {
        return next(1);
    }

    public long next(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be > 0");
        }
        // 假如为 13
        long nextValue = this.nextValue;
        // 申请 n 个序号，假如为7，则申请的序号就是 20
        long nextSequence = nextValue + n;
        // 现在要判断序号为20的数据是否可用
        // 假如 bufferSize = 8，
        // wrapPoint = 20 - 8 = 12，也就是说生产者现在申请的序号是 第三圈的第四个元素及之前的元素
        // 想要不覆盖元素，消费者的进度必须在13，生产者才能申请这些序号，那么如果消费者不到13，生产者就要阻塞
        long wrapPoint = nextSequence - bufferSize;
        // 一直阻塞到消费者把这里的数据全都消费了再申请
        while (wrapPoint > consumer.get()) {
            LockSupport.parkNanos(1L);
        }
        this.nextValue = nextSequence;
        return nextSequence;
    }

    public ProcessingSequenceBarrier newBarrier() {
        return new ProcessingSequenceBarrier(waitStrategy, cursor);
    }

    /**
     * 把消费者的进度赋值给 consumer变量
     * @param sequence
     */
    public final void addGatingSequences(Sequence sequence) {
        this.consumer = sequence;
    }
    public void publish(long sequence) {
        cursor.setValue(sequence);
    }
}
