package com.xiaohe.provider;

/**
 * @author : 小何
 * @Description :
 * @date : 2023-12-25 16:25
 */
public class RingBuffer<E> implements DataProvider<E> {
    /**
     * 掩码，数组长度-1
     */
    private final long indexMask;

    private final Object[] entries;
    /**
     * 环形数组的容量
     */
    protected final int bufferSize;

    public RingBuffer(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("bufferSize必须大于0");
        }
        if (Integer.bitCount(bufferSize) != 1) {
            throw new IllegalArgumentException("bufferSize必须是2的次幂");
        }
        this.bufferSize = bufferSize;
        this.entries = new Object[bufferSize];
        this.indexMask = bufferSize - 1;
    }
    private <A> void translateAndPublish(EventTranslatorOneArg<E, A> translator, long sequence, A arg0) {
        try {
            // 拿到对应元素，一般为 Request
            E request = get(sequence);
            // 将 arg0 塞到 Request 中
            translator.translateTo(request, sequence, arg0);
        } catch (Exception e) {

        }
    }

    /**
     * 将数组填充为无效的 Request 数据
     * @param eventFactory
     */
    private void fill(EventFactory<E> eventFactory) {
        for (int i = 0; i < bufferSize; i++) {
            entries[i] = eventFactory.newInstance();
        }
    }

    public <A> void publishEvent(EventTranslatorOneArg<E, A> translator, A arg0) {
        // 获取下一个可用序号
        final long sequence = next();
        // 给对应位置的Request填充上arg0
        translateAndPublish(translator, sequence, arg0);

    }
    /**
     * 获得生产者的下一个可用序号
     * @return
     */
    public long next() {
        return 0;
    }
    public E get(long sequence) {
        return elementAt(sequence);
    }

    protected final E elementAt(long sequence) {
        return (E) entries[(int) (sequence & indexMask)];
    }
}
