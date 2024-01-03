package com.xiaohe.provider;

import com.xiaohe.exception.InsufficientCapacityException;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-01 14:04
 */
public interface Sequenced {
    int getBufferSize();

    /**
     * 查看是否有指定数量的可用序号
     * @param requiredCapacity
     * @return
     */
    boolean hasAvailableCapacity(int requiredCapacity);

    /**
     * 剩余数量
     * @return
     */
    long remainingCapacity();

    long next();

    long next(int n);

    long tryNext() throws InsufficientCapacityException;

    long tryNext(int n) throws InsufficientCapacityException;

    void publish(long sequence);

    void publish(long lo, long hi);

}
