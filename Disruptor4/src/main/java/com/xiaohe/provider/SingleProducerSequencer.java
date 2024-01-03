package com.xiaohe.provider;

import com.xiaohe.common.Sequence;
import com.xiaohe.exception.InsufficientCapacityException;
import com.xiaohe.util.Util;
import com.xiaohe.util.wait.WaitStrategy;

import java.util.concurrent.locks.LockSupport;

/**
 * @author : 小何
 * @Description :
 * @date : 2024-01-03 15:14
 */
abstract class SingleProducerSequencerPad extends AbstractSequencer {
    protected long p1, p2, p3, p4, p5, p6, p7;

    SingleProducerSequencerPad(int bufferSize, WaitStrategy waitStrategy) {
        super(bufferSize, waitStrategy);
    }
}

abstract class SingleProducerSequencerFields extends SingleProducerSequencerPad {
    SingleProducerSequencerFields(int bufferSize, WaitStrategy waitStrategy) {
        super(bufferSize, waitStrategy);
    }

    /**
     * 当前分配的可用的序号
     */
    long nextValue = Sequence.INITIAL_VALUE;

    /**
     * gatingSequences 数组中消费最慢的消费者的进度
     */
    long cachedValue = Sequence.INITIAL_VALUE;
}

public class SingleProducerSequencer extends SingleProducerSequencerFields {
    protected long p1, p2, p3, p4, p5, p6, p7;
    public SingleProducerSequencer(int bufferSize, WaitStrategy waitStrategy) {
        super(bufferSize, waitStrategy);
    }

    @Override
    public boolean hasAvailableCapacity(int requiredCapacity) {
        return hasAvailableCapacity(requiredCapacity, false);
    }
    private boolean hasAvailableCapacity(int requiredCapacity, boolean doStore) {
        long nextValue = this.nextValue;
        long cachedGatingSequence = this.cachedValue;
        long wrapPoint = (nextValue + requiredCapacity) - bufferSize;
        if (wrapPoint > cachedGatingSequence || cachedGatingSequence > nextValue) {
            // 更新生产者的最新进度
            if (doStore) {
                cursor.setVolatile(nextValue);
            }
            this.cachedValue = Util.getMinimumSequence(gatingSequences, nextValue);
            if (wrapPoint > cachedGatingSequence) {
                return false;
            }
        }
        return true;
    }

    @Override
    public long remainingCapacity() {
        return 0;
    }

    @Override
    public long next() {
        return next(1);
    }

    @Override
    public long next(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("n must > 0");
        }
        // 当前进度
        long nextValue = this.nextValue;
        // 下一个想要申请的序号
        long nextSequence = nextValue + n;
        long wrapPoint = nextSequence - bufferSize;
        long cachedGatingSequence = this.cachedValue;
        if (wrapPoint > cachedGatingSequence || cachedGatingSequence > nextValue) {
            // 更新生产者进度
            cursor.setVolatile(nextValue);
            long minSequence;
            while (wrapPoint > (minSequence = Util.getMinimumSequence(gatingSequences, nextValue))) {
                LockSupport.parkNanos(1L);
            }
            this.nextValue = nextSequence;
        }
        return nextSequence;
    }

    @Override
    public long tryNext() throws InsufficientCapacityException {
        return tryNext(1);
    }

    @Override
    public long tryNext(int n) throws InsufficientCapacityException {
        if (n < 1) {
            throw new IllegalArgumentException("n must > 0");
        }
        if (!hasAvailableCapacity(n, true)) {
            throw InsufficientCapacityException.INSTANCE;
        }
        long nextSequence = this.nextValue += n;
        return nextSequence;
    }

    @Override
    public void publish(long sequence) {
        cursor.set(sequence);
        waitStrategy.signalAllWhenBlocking();
    }

    @Override
    public void publish(long lo, long hi) {
        publish(hi);
    }

    @Override
    public void claim(long sequence) {
        this.nextValue = sequence;
    }

    @Override
    public boolean isAvailable(long sequence) {
        return sequence <= cursor.get();
    }

    @Override
    public long getHighestPublishedSequence(long nextSequence, long availableSequence) {
        return availableSequence;
    }
}
