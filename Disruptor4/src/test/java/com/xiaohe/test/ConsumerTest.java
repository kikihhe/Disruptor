package com.xiaohe.test;

import com.xiaohe.Disruptor;
import com.xiaohe.consumer.batch.EventHandler;
import com.xiaohe.consumer.handler.ExceptionHandler;
import com.xiaohe.model.Event;
import com.xiaohe.model.SimpleEventFactory;
import com.xiaohe.model.SimpleEventHandler;
import com.xiaohe.model.SimpleExceptionHandler;
import com.xiaohe.provider.ProducerType;
import com.xiaohe.provider.translator.EventTranslatorOneArg;
import com.xiaohe.util.wait.impl.SleepingWaitStrategy;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author : 小何
 * @Description : 单生产者，多消费者并行消费
 * @date : 2024-01-05 21:01
 */
public class ConsumerTest {
    public static void main(String[] args) {
        // 事件生产工厂, 生产String
        SimpleEventFactory<String> eventFactory = new SimpleEventFactory<>();
        int ringBufferSize = 128;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        SleepingWaitStrategy waitStrategy = new SleepingWaitStrategy();

        // 创建 Disruptor, 使用单生产者模式
        Disruptor<Event<String>> disruptor = new Disruptor<>(
                eventFactory,
                ringBufferSize,
                threadFactory,
                ProducerType.SINGLE, waitStrategy
        );
//
//        // 创建消费者处理器，多个消费者
//        EventHandler<Event<String>> eventHandler1 = new SimpleEventHandler<>(1);
//        EventHandler<Event<String>> eventHandler2 = new SimpleEventHandler<>(2);
//        EventHandler<Event<String>> eventHandler3 = new SimpleEventHandler<>(3);
//        EventHandler<Event<String>> eventHandler4 = new SimpleEventHandler<>(4);
//
//        // 把所有消费者放入 EventHandlerGroup
//        disruptor.handleEventsWith(eventHandler1, eventHandler2, eventHandler3, eventHandler4);

        // 设置异常处理器
        ExceptionHandler<Event<String>> exceptionHandler = new SimpleExceptionHandler<>();
        disruptor.setDefaultExceptionHandler(exceptionHandler);

        // 启动 Disruptor
        // 虽然启动了所有的消费者程序，但是此时并没有消息，消费者都会阻塞等待生产者发布数据
        disruptor.start();

        EventTranslatorOneArg<Event<String>, String> eventTranslatorOneArg =
                (event, sequence, arg0) -> event.setData(arg0);

        for (int i = 0; i < 10; i++) {
            disruptor.publishEvent(eventTranslatorOneArg, "数据" + i);
        }

        disruptor.shutdown();
    }
}
