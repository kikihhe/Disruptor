package com.xiaohe.provider.sequencer;

import com.xiaohe.exception.InsufficientCapacityException;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-02-05 22:02
 */
public interface Sequenced {
    int getBufferSize();

    /**
     * 查看是否有足量的可用序号
     * @param requireCapacity
     * @return
     */
    boolean hasAvailableCapacity(int requireCapacity);

    /**
     * 剩余容量
     * @return
     */
    public long remainingCapacity();
    public long next();
    public long next(int n);
    public long tryNext() throws InsufficientCapacityException;
    public long tryNext(int n) throws InsufficientCapacityException;

    /**
     * 发布数据
     * @param sequence
     */
    public void publish(long sequence);

    /**
     * 批量发布
     * @param low
     * @param high
     */
    public void publish(long low, long high);
}
