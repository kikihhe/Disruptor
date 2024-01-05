package com.xiaohe.consumer.batch;


import com.xiaohe.DataProvider;
import com.xiaohe.common.Sequence;
import com.xiaohe.consumer.EventProcessor;
import com.xiaohe.consumer.SequenceBarrier;
import com.xiaohe.consumer.handler.TimeoutHandler;
import com.xiaohe.consumer.aware.BatchStartAware;
import com.xiaohe.consumer.aware.LifecycleAware;
import com.xiaohe.exception.AlertException;
import com.xiaohe.consumer.handler.ExceptionHandler;
import com.xiaohe.consumer.handler.FatalExceptionHandler;


import java.util.concurrent.TimeoutException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-03 18:34
 */
public final class BatchEventProcessor<T> implements EventProcessor {
    /**
     * 空闲状态
     */
    private static final int IDLE = 0;

    /**
     * 停止状态
     */
    private static final int HALTED = IDLE + 1;

    /**
     * 运行状态
     */
    private static final int RUNNING = HALTED + 1;
    /**
     * 此消费者的状态，默认空闲
     */
    private final AtomicInteger running = new AtomicInteger(IDLE);

    /**
     * 异常处理器
     */
    private ExceptionHandler<? super T> exceptionHandler = new FatalExceptionHandler();
    /**
     * 获取数据
     */
    private final DataProvider<T> dataProvider;
    /**
     * 序号屏障
     */
    private final SequenceBarrier sequenceBarrier;

    /**
     * 这个消费者线程在拿到消息后的处理逻辑
     */
    private final EventHandler<? super T> eventHandler;

    /**
     * 此消费者自己的消费序号
     */
    private final Sequence sequence = new Sequence(Sequence.INITIAL_VALUE);

    /**
     * 超时处理器。可以由用户自定义
     * 用户只需要在实现 EventHandler 的同时实现这个接口即可。
     */
    private final TimeoutHandler timeoutHandler;

    /**
     * 消费者开始真正工作的回调接口，由用户扩展。
     * 用户只需要在实现 EventHandler 的同时实现这个接口即可。
     */
    private final BatchStartAware batchStartAware;

    public BatchEventProcessor(final DataProvider<T> dataProvider,
                               final SequenceBarrier sequenceBarrier,
                               final EventHandler<? super T> eventHandler) {
        this.dataProvider = dataProvider;
        this.sequenceBarrier = sequenceBarrier;
        this.eventHandler = eventHandler;
        batchStartAware = (eventHandler instanceof BatchStartAware) ? (BatchStartAware) eventHandler : null;
        timeoutHandler = eventHandler instanceof TimeoutHandler ? (TimeoutHandler) eventHandler : null;
    }

    /**
     * 得到此消费者的进度
     */
    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void halt() {
        running.set(HALTED);
        sequenceBarrier.alert();
    }

    @Override
    public boolean isRunning() {
        return running.get() != IDLE;
    }

    @Override
    public void run() {
        // 将启动为设置为 RUNNING
        if (running.compareAndSet(IDLE, RUNNING)) {
            sequenceBarrier.clearAlert();
            // 第一次启动，执行生命周期的启动方法
            notifyStart();
            try {
                // 再次判断
                if (running.get() == RUNNING) {
                    processEvents();
                }
            } finally {
                notifyShutdown();
                running.set(IDLE);
            }
        } else {
            if (running.get() == RUNNING) {
                throw new IllegalStateException("Thread is already running");
            } else {
                earlyExit();
            }
        }
    }

    private void processEvents() {
        T event = null;
        // 想要消费 nextSequence 位置的数据
        long nextSequence = sequence.get() + 1L;
        while (true) {
            try {
                final long availableSequence = sequenceBarrier.waitFor(nextSequence);
                // 每一次批量执行的时候都会执行这个方法
                if (batchStartAware != null) {
                    batchStartAware.onBatchStart(availableSequence - nextSequence + 1);
                }
                while (nextSequence <= availableSequence) {
                    event = dataProvider.get(nextSequence);
                    eventHandler.onEvent(event, nextSequence, nextSequence == availableSequence);
                    nextSequence++;
                }
                // 本次批量消费结束，设置一下消费进度
                sequence.set(availableSequence);
            } catch (final TimeoutException e) {
                notifyTimeout(sequence.get());
            } catch (final AlertException ex) {
                if (running.get() != RUNNING) {
                    break;
                }
            } catch (final Throwable ex) {
                // 如果是本次消费出现了异常，就处理一下，下次还是要消费的。
                exceptionHandler.handleEventException(ex, nextSequence, event);
                sequence.set(nextSequence);
                nextSequence++;
            }
        }
    }

    /**
     * 设置异常处理器
     *
     * @param exceptionHandler
     */
    public void setExceptionHandler(final ExceptionHandler<? super T> exceptionHandler) {
        if (null == exceptionHandler) {
            throw new NullPointerException();
        }
        this.exceptionHandler = exceptionHandler;
    }

    /**
     * 处理超时异常
     *
     * @param availableSequence
     */
    private void notifyTimeout(final long availableSequence) {
        try {
            if (timeoutHandler != null) {
                timeoutHandler.onTimeout(availableSequence);
            }
        } catch (Throwable e) {
            exceptionHandler.handleEventException(e, availableSequence, null);
        }
    }
    private void earlyExit() {
        notifyStart();
        notifyShutdown();
    }
    /**
     * 如果实现了 LifecycleAware 方法，就执行开始方法
     */
    private void notifyStart() {
        if (eventHandler instanceof LifecycleAware) {
            try {
                ((LifecycleAware) eventHandler).onStart();
            } catch (final Throwable ex) {
                exceptionHandler.handleOnStartException(ex);
            }
        }
    }

    /**
     * 如果实现了 LifecycleAware 方法，就执行结束方法
     */
    private void notifyShutdown() {
        if (eventHandler instanceof LifecycleAware) {
            try {
                ((LifecycleAware) eventHandler).onShutdown();
            } catch (final Throwable e) {
                exceptionHandler.handleOnShutdownException(e);
            }
        }
    }
}
