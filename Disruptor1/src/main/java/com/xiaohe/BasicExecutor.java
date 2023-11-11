package com.xiaohe;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * @author : 小何
 * @Description : 线程工厂，为消费者创建现成的。每一个消费者都会被包装为一个BatchEventProcessor
 * BatchEventProcessor继承了Runnable
 * @date : 2023-11-11 12:19
 */
public class BasicExecutor implements Executor {
    /**
     * 创建线程的工厂
     */
    private final ThreadFactory threadFactory;
    /**
     * 存储了所有消费者线程，方便输出
     */
    private final Queue<Thread> threads = new ConcurrentLinkedQueue<>();

    public BasicExecutor(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * 给BatchEventProcessor创建线程、启动、加入队列
     * @param command the runnable task
     */
    @Override
    public void execute(Runnable command) {
        final Thread thread = threadFactory.newThread(command);
        if (null == thread) {
            throw new RuntimeException("Failed to create thread to run: " + command);
        }
        // 启动线程
        thread.start();
        // 加到线程队列中
        threads.add(thread);

    }

    @Override
    public String toString() {
        return "BasicExecutor{" +
                "threads=" + dumpThreadInfo() +
                "}";
    }
    private String dumpThreadInfo() {
        final StringBuilder sb = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for (Thread t : threads) {
            ThreadInfo threadInfo = threadMXBean.getThreadInfo(t.getId());
            sb.append("{");
            sb.append("name=").append(t.getName()).append(",");
            sb.append("id=").append(t.getId()).append(",");
            sb.append("state=").append(threadInfo.getThreadState()).append(",");
            sb.append("lockInfo=").append(threadInfo.getLockInfo());
            sb.append("}");
        }
        return sb.toString();
    }
}
