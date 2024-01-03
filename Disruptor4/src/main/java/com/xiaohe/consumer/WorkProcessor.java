package com.xiaohe.consumer;

import com.xiaohe.RingBuffer;
import com.xiaohe.common.Sequence;
import com.xiaohe.exception.AlertException;
import com.xiaohe.exception.handler.ExceptionHandler;
import com.xiaohe.provider.EventReleaseAware;
import com.xiaohe.provider.EventReleaser;

import java.sql.Time;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-03 19:59
 */
public class WorkProcessor<T> implements EventProcessor{
    /**
     * 此线程是否正在工作
     */
    private final AtomicBoolean running = new AtomicBoolean(false);
    /**
     * 此线程的序号
     */
    private final Sequence sequence = new Sequence(Sequence.INITIAL_VALUE);

    private final RingBuffer<T> ringBuffer;

    private final SequenceBarrier sequenceBarrier;

    private final WorkHandler<? super T> workHandler;

    private final ExceptionHandler<? super T> exceptionHandler;

    /**
     * WorkerPool自身的消费进度
     */
    private final Sequence workSequence;

    private final EventReleaser eventReleaser = new EventReleaser() {
        @Override
        public void release() {
            sequence.set(Long.MAX_VALUE);
        }
    };
    // 超时处理器
    private final TimeoutHandler timeoutHandler;

    public WorkProcessor(final RingBuffer<T> ringBuffer,
                         final SequenceBarrier sequenceBarrier,
                         final WorkHandler<? super T> workHandler,
                         final ExceptionHandler<? super T> exceptionHandler,
                         final Sequence workSequence) {
        this.ringBuffer = ringBuffer;
        this.sequenceBarrier = sequenceBarrier;
        this.workHandler = workHandler;
        this.workSequence = workSequence;
        this.exceptionHandler = exceptionHandler;
        if (this.workHandler instanceof EventReleaseAware) {
            ((EventReleaseAware)this.workHandler).setEventReleaser(eventReleaser);
        }
        timeoutHandler = (workHandler instanceof TimeoutHandler) ? (TimeoutHandler) workHandler : null;
    }
    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void halt() {
        running.set(false);
        sequenceBarrier.alert();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public void run() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Thread is already running");
        }
        sequenceBarrier.clearAlert();
        notifyStart();
        // 是否可以去申请下一个序号
        boolean processedSequence = true;
        long cachedAvailableSequence = Long.MIN_VALUE;
        // 消费者的当前进度
        long nextSequence = sequence.get();

        T event = null;
        while (true) {
            try {
                // 1. 判断一下是否可以去申请下一个序号。如果可以就去申请，可能有并发情况导致不止申请一次。
                // 2. 消费完一个数据就可以再次申请了，直到 cachedAvailableSequence 不够用，就去更新这个值
                if (processedSequence) {
                    processedSequence = false;
                    do {
                        // 1. 得到应该消费的进度
                        nextSequence = workSequence.get() + 1L;
                        // 1. 虽然得到了但是还没有消费，所以自己的进度还不能变
                        sequence.set(nextSequence - 1L);
                        // 1. 如果CAS成功了，说明这个消费者在并发情况下申请到了这个序号，那么就可以去消费了
                    } while (!workSequence.compareAndSet(nextSequence - 1L, nextSequence));
                }
                // 1. 第一次不会走这里
                // 2. 第二遍会直接走这里，判断一般都会进去
                if (cachedAvailableSequence >= nextSequence) {
                    // 拿到数据，消费
                    event = ringBuffer.get(nextSequence);
                    workHandler.onEvent(event);
                    // 下一次可以申请了
                    processedSequence = true;
                } else {
                    // 1. cachedAvailableSequence在第一次时为 Long.MIN_VALUE，会走这里。更新一下 cachedAvailableSequence，这个值是真正可以消费的数据下标
                    cachedAvailableSequence = sequenceBarrier.waitFor(nextSequence);
                }
            } catch (final TimeoutException e) {
                notifyTimeout(sequence.get());
            } catch (final AlertException ex) {
                if (!running.get()) {
                    break;
                }
            } catch (final Throwable ex) {
                //处理异常
                exceptionHandler.handleEventException(ex, nextSequence, event);
                //置为true，方便进入下一次循环争抢可消费序号
                processedSequence = true;
            }
        }
        //走到这里说明退出循环了，也就意味着消费者处理器要停止工作了
        notifyShutdown();
        running.set(false);
    }

    private void notifyTimeout(final long availableSequence) {
        try {
            if (timeoutHandler != null) {
                timeoutHandler.onTimeout(availableSequence);
            }
        } catch (Throwable e) {
            exceptionHandler.handleEventException(e, availableSequence, null);
        }
    }

    private void notifyStart() {
        if (workHandler instanceof LifecycleAware) {
            try {
                ((LifecycleAware) workHandler).onStart();
            } catch (final Throwable ex) {
                exceptionHandler.handleOnStartException(ex);
            }
        }
    }

    private void notifyShutdown() {
        if (workHandler instanceof LifecycleAware) {
            try {
                ((LifecycleAware) workHandler).onShutdown();
            } catch (final Throwable ex) {
                exceptionHandler.handleOnShutdownException(ex);
            }
        }
    }
}
