package com.xiaohe;

import com.xiaohe.strategy.wait.WaitStrategy;
import com.xiaohe.strategy.wait.impl.SleepingWaitStrategy;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 11:49
 */
public class ArrayBlockingQueue<E> {
    final Object[] items;

    private int count;

    private int putIndex;

    private int takeIndex;
    /**
     * 阻塞策略
     */
    private WaitStrategy waitStrategy = new SleepingWaitStrategy();

    public ArrayBlockingQueue(int capacity) {
        this.items = new Object[capacity];
    }

    public ArrayBlockingQueue(int capacity, WaitStrategy waitStrategy) {
        this.items = new Object[capacity];
        this.waitStrategy = waitStrategy;
    }

    /**
     * 存放数据到队列中的方法，不限时阻塞，一直到放入为止
     * @param e
     */
    public void put(E e) {
        while (count == items.length) {
            waitStrategy.waitFor();
        }
        System.out.println("队列中有位置了，消费者线程被唤醒");
        waitStrategy.setRetries(200);

        enqueue(e);
    }

    /**
     * 从队列中取元素，不限时阻塞，一直到取出为止
     * @return
     */
    public E take() {
        while (count == 0) {
            waitStrategy.waitFor();
        }
        System.out.println("队列中有元素了，消费者线程醒来");
        waitStrategy.setRetries(200);
        return dequeue();
    }

    private void enqueue(E e) {
        final Object[] items = this.items;
        items[putIndex] = e;
        if (++putIndex == items.length) {
            putIndex = 0;
        }
        count++;
    }
    private E dequeue() {
        final Object[] items = this.items;
        E e = (E) items[takeIndex];
        items[takeIndex] = null;
        if (++takeIndex == items.length) {
            takeIndex = 0;
        }
        count--;
        return e;
    }
}
