package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-01 14:03
 */
public interface Cursored {
    /**
     * 获得当前生产者的序号
     * @return
     */
    long getCursor();
}
