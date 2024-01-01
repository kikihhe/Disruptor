package test;

import com.xiaohe.Sequence;
import com.xiaohe.consumer.EventHandler;
import com.xiaohe.consumer.ProcessingSequenceBarrier;
import com.xiaohe.consumer.impl.BatchEventProcessor;
import com.xiaohe.handler.SimpleEventHandler;
import com.xiaohe.provider.*;
import com.xiaohe.provider.impl.SingleProducerSequencer;
import com.xiaohe.wait.SleepingWaitStrategy;
import com.xiaohe.wait.WaitStrategy;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-26 21:44
 */
public class Test {
    public static void main(String[] args) {
        // 生产工厂
        SimpleEventFactory<Integer> eventFactory = new SimpleEventFactory<>();
        int ringBufferSize = 128;
        // 生产者序号屏蔽
        SingleProducerSequencer sequencer = new SingleProducerSequencer(ringBufferSize, 消费者进度);
        RingBuffer buffer = new RingBuffer(ringBufferSize, eventFactory, sequencer);
        // 传输器对象
        EventTranslatorOneArg<Request<Integer>, Integer> eventTranslatorOneArg = new EventTranslatorOneArg<Request<Integer>, Integer>() {
            @Override
            public void translateTo(Request<Integer> event, long sequence, Integer arg0) {
                event.setData(arg0);
            }
        };
        // 生产者发布128条数据
        for (int i = 0; i < 128; i++) {
            buffer.publishEvent(eventTranslatorOneArg, i);
        }

        DataProvider dataProvider = buffer;

        // 创建一个消费者逻辑
        EventHandler<Request<Integer>> eventHandler = new SimpleEventHandler<>();
        WaitStrategy waitStrategy = (WaitStrategy) new SleepingWaitStrategy();
        Sequence cursorSequence = SingleProducerSequencer.get("生产者进度");
        ProcessingSequenceBarrier barrier = new ProcessingSequenceBarrier(waitStrategy, cursorSequence);
        BatchEventProcessor processor = new BatchEventProcessor(eventHandler, dataProvider, barrier);
        new Thread(processor).start();
    }
}
