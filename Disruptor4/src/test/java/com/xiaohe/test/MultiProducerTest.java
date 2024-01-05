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
 * @Description :
 * @date : 2024-01-05 23:16
 */
public class MultiProducerTest {
    public static void main(String[] args) throws InterruptedException {
        SimpleEventFactory<String> eventFactory = new SimpleEventFactory<>();
        int ringBufferSize = 128;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        WaitStrategy waitStrategy = new SleepingWaitStrategy();

        // 创建 Disruptor
        Disruptor<Event<String>> disruptor = new Disruptor<>(
                eventFactory,
                ringBufferSize,
                threadFactory,
                ProducerType.MULTI,
                waitStrategy
        );

        // 创建几个消费者
        WorkHandler<Event<String>> eventHandler1 = new WorkerEventHandler<>();
        WorkHandler<Event<String>> eventHandler2 = new WorkerEventHandler<>();
        WorkHandler<Event<String>> eventHandler3 = new WorkerEventHandler<>();
        WorkHandler<Event<String>> eventHandler4 = new WorkerEventHandler<>();
        WorkHandler<Event<String>> eventHandler5 = new WorkerEventHandler<>();
        WorkHandler<Event<String>> eventHandler6 = new WorkerEventHandler<>();

        disruptor.handleEventsWithWorkerPool(eventHandler1, eventHandler2, eventHandler3, eventHandler4, eventHandler5, eventHandler6);

        // 创建异常处理器
        ExceptionHandler<Event<String>> exceptionHandler = new SimpleExceptionHandler<>();
        disruptor.setDefaultExceptionHandler(exceptionHandler);

        // 启动消费者
        disruptor.start();

        EventTranslatorOneArg<Event<String>, String> eventTranslatorOneArg = (event, sequence, arg0) -> {
            event.setData(arg0);
        };

        // 整两个生产者线程发布数据
        for (int i = 0; i < 20; i++) {
            // 每个生产者线程发布10条数据
            new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    disruptor.publishEvent(eventTranslatorOneArg, Thread.currentThread().getName() + "发布的第" + (j+1) + "条数据");
                }
            }, "生产者" + (i+1)).start();
        }


        // 异步生产、消费数据，让主线程睡一会
        Thread.sleep(10000);
        disruptor.shutdown();
    }
}
