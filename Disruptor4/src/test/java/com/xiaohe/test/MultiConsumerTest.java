package com.xiaohe.test;

import com.xiaohe.Disruptor;
import com.xiaohe.consumer.handler.ExceptionHandler;
import com.xiaohe.consumer.multi.WorkHandler;
import com.xiaohe.model.Event;
import com.xiaohe.model.SimpleEventFactory;
import com.xiaohe.model.SimpleExceptionHandler;
import com.xiaohe.model.WorkerEventHandler;
import com.xiaohe.provider.ProducerType;
import com.xiaohe.provider.translator.EventTranslatorOneArg;
import com.xiaohe.util.wait.WaitStrategy;
import com.xiaohe.util.wait.impl.SleepingWaitStrategy;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author : 小何
 * @Description : 单生产者，多消费者并发消费
 * @date : 2024-01-05 21:23
 */
public class MultiConsumerTest {
    public static void main(String[] args) {
        SimpleEventFactory<String> eventFactory = new SimpleEventFactory<>();
        int ringBufferSize = 128;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        WaitStrategy waitStrategy = new SleepingWaitStrategy();

        // 创建 Disruptor, 使用单生产者模式
        Disruptor<Event<String>> disruptor = new Disruptor<>(
                eventFactory,
                ringBufferSize,
                threadFactory,
                ProducerType.SINGLE, waitStrategy
        );

        // 创建两个消费者
        WorkHandler<Event<String>> workHandler1 = new WorkerEventHandler<>();
        WorkHandler<Event<String>> workHandler2 = new WorkerEventHandler<>();

        // 创建 WorkerPool
        disruptor.handleEventsWithWorkerPool(workHandler1, workHandler2);

        // 创建异常处理器
        ExceptionHandler<Event<String>> exceptionHandler = new SimpleExceptionHandler<>();
        disruptor.setDefaultExceptionHandler(exceptionHandler);

        disruptor.start();

        // 传输器
        EventTranslatorOneArg<Event<String>, String> translatorOneArg = (event, sequence, arg0) -> {
            event.setData(arg0);
        };

        for (int i = 0; i < 10; i++) {
            disruptor.publishEvent(translatorOneArg, "第" + (i+1) + "条数据");
        }

        // 关闭 Disruptor
        disruptor.shutdown();
    }
}
