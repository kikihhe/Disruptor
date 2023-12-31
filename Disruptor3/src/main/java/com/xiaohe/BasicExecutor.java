package com.xiaohe;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-26 23:22
 */
public class BasicExecutor implements Executor {
    /**
     * 线程工厂
     */
    private final ThreadFactory factory;
    /**
     * 所有消费者线程
     */
    private final Queue<Thread> threads = new ConcurrentLinkedQueue<>();

    public BasicExecutor(ThreadFactory factory) {
        this.factory = factory;
    }

    @Override
    public void execute(Runnable command) {
        Thread thread = factory.newThread(command);
        if (null == thread) {
            throw new RuntimeException("Failed to create thread to run: " + command);
        }
        thread.start();
        threads.add(thread);
    }


    /**
     * 输出线程队列中的所有线程
     * @return
     */
    private String dumpThreadInfo(){
        final StringBuilder sb = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        for (Thread t : threads){
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
    @Override
    public String toString(){

        return "BasicExecutor{" +
                "threads=" + dumpThreadInfo() +
                '}';
    }

}
