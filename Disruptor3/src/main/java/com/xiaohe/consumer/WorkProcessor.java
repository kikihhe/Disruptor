package com.xiaohe.consumer;

import com.xiaohe.Sequence;
import com.xiaohe.provider.RingBuffer;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : 小何
 * @Description : 并发消费者
 * @date : 2023-12-31 22:33
 */
public class WorkProcessor<T> implements EventProcessor {
    /**
     * 判断此消费者线程是否在工作
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 此消费者的进度
     */
    private final Sequence sequence = new Sequence(Sequence.INITIAL_VALUE);
    /**
     * 环形数组
     */
    private final RingBuffer<T> ringBuffer;

    /**
     * 序号屏障
     */
    private final ProcessingSequenceBarrier sequenceBarrier;

    /**
     * 用户定义的消费行为
     */
    private final WorkHandler<? extends T> workHandler;

    /**
     * 用户定义的出现异常后的行为
     */
    private final ExceptionHandler<? extends T> exceptionHandler;
    /**
     * WorkerPool的消费进度
     */
    private final Sequence workSequence;


    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void run() {
        // 判断此消费者线程是否可以去申请下一个消费序号
        boolean processedSequence = true;
        // 当前生产者的可用数据序号
        long cacheAvailableSequence = Long.MIN_VALUE;
        // 得到当前消费者的消费进度
        long nextSequence = sequence.get();
        T event = null;
        // 循环中有两段逻辑，使用 1、2 标识
        while (true) {
            // 1. 消费者刚启动时，默认可以去申请消费进度，但是由于不知道生产者是否启动，所以申请之后要等待生产者发布数据
            // 2. 第二遍循环，processedSequence 是 false，因为这时候刚等生产者发布数据。不会走这里
            if (processedSequence) {
                // 1. 申请第一个之后设置为false
                processedSequence = false;
                do {
                    // 1. 得到下一个可以消费的序号, 刚启动时，workSequence中的值一定是-1，那nextSequence就是0
                    // 1. 万一这次申请失败，也就是CAS失败，再次申请下一个序号
                    nextSequence = workSequence.get() + 1L;
                    // 1. 给当前消费者的进度赋值为-1，只有真正消费成功了才可以修改sequence的值
                    sequence.set(nextSequence - 1L);
                    // 使用CAS操作将 workSequence 从 nextSequence - 1 修改为 nextSequence
                } while (!workSequence.compareAndSet(nextSequence - 1L, nextSequence));
                // 1. 走到这里，sequence = -1, nextSequence = 0, workSequence = 0
            }
            // 1. 判断之前得到的 cacheAvailableSequence 是否大于 nextSequence, 注意，此时 nextSequence = 0
            // 1. 第一次循环时，cacheAvailableSequence = Long.MIN_VALUE, 一定小于将要消费的进度，，所以不会执行该步骤
            // 2. 第二次走到这里，生产者已经发布了数据，可以进入执行。
            if (cacheAvailableSequence >= nextSequence) {
                // 得到数据，将其消费，将申请权限设置为true
                event = ringBuffer.get(nextSequence);
                workHandler.onEvent(event);
                // 如果此次消费了，就将申请权限打开，可以去申请下一个。
                processedSequence = true;
            } else {
                // 1. 消费者刚启动，生产者还未发布数据，所以要等待
                // 1. 等待后，序号屏障会返回当前可消费的最大下标
                // 2. 不只是消费者刚启动时会执行这里，每次生产者的数据不够时，消费者都要在这里等待。
                cacheAvailableSequence = sequenceBarrier.waitFor(nextSequence);
            }

        }
    }
}
