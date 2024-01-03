package com.xiaohe.consumer;

import com.xiaohe.common.Sequence;

import java.util.concurrent.Executor;

/**
 * @author : 小何
 * @Description : 封装消费者信息
 * @date : 2024-01-03 21:39
 */
public interface ConsumerInfo {
    // 来获得消费者的消费进度的，也就是消费者消费到哪个序号了
    Sequence[] getSequences();

    // 来得到消费者的序号屏障
    SequenceBarrier getBarrier();

    // 判断当前消费者是否为消费者序列中的最后一个消费者，也就是消费进度最慢的消费者
    // 如果有5个消费者都在消费环形数组中的数据，现在这5个消费者消费的进度不同，有的消费到数组的第5个索引位置了，而最慢的才消费到第3个索引位置
    // 如果生产者想往环形数组中添加数据，必须要等消费最慢的那个消费者把第3索引的数据消费了，才能使用第3索引的位置存放数据
    boolean isEndOfChain();

    // 启动程序的方法，启动所有消费线程
    void start(Executor executor);

    // 终止线程工作
    void halt();

    // 如果当前消费者是最慢消费者，但这时候有比它更慢的了
    // 这个方法就会把当前消费者的最慢消费者身份更改掉，表示它不是最慢消费者了
    void markAsUsedInBarrier();

    // 判断当前消费者线程是否还在运行
    boolean isRunning();


}
