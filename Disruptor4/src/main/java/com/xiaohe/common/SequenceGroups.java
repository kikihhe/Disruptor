package com.xiaohe.common;

import com.xiaohe.provider.Cursored;

import java.util.SimpleTimeZone;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.util.Arrays.copyOf;
import static java.util.Arrays.setAll;

/**
 * @author : 小何
 * @Description : 管理序号组的类，提供了对一组序号进行操作的功能
 * @date : 2024-01-01 22:50
 */
public class SequenceGroups {

    /**
     * 线程安全的添加 Sequence 到一个 Sequence组中。使用CAS操作完成
     * @param holder 拥有原先序号组的类，从中取出老序号组，再将新序号组设置进去
     * @param updater 线程安全的操作器
     * @param cursor 获取生产者序号
     * @param sequencesToadd 需要添加的序号
     * @param <T>
     */
    public static <T> void addSequences(final T holder,
                                        final AtomicReferenceFieldUpdater<T, Sequence[]> updater,
                                        final Cursored cursor,
                                        final Sequence... sequencesToadd) {
        // 生产者的序号
        long cursorSequence;
        // 添加后的序号组
        Sequence[] updatedSequences;
        // 添加前的序号组
        Sequence[] currentSequences;
        do {
            // 从holder中取出老序号组
            currentSequences = updater.get(holder);
            // 创建新序号组，大小为 老序号组大小+添加序号组大小
            updatedSequences = copyOf(currentSequences, currentSequences.length + sequencesToadd.length);
            // 当前生产者进度
            cursorSequence = cursor.getCursor();
            int index = currentSequences.length;
            for (Sequence sequence : sequencesToadd) {
                sequence.set(cursorSequence);
                updatedSequences[index++] = sequence;
            }
        } while (!updater.compareAndSet(holder, currentSequences, updatedSequences));
        cursorSequence = cursor.getCursor();
        for (Sequence sequence : sequencesToadd) {
            sequence.set(cursorSequence);
        }

    }

    public static <T> boolean removeSequence(
            final T holder,
            final AtomicReferenceFieldUpdater<T, Sequence[]> sequenceUpdater,
            final Sequence sequence) {

        int numToRemove;
        Sequence[] oldSequences;
        Sequence[] newSequences;

        do {
            oldSequences = sequenceUpdater.get(holder);
            // 需要移除的Sequence数量
            numToRemove = countMatching(oldSequences, sequence);

            if (0 == numToRemove) {
                break;
            }

            final int oldSize = oldSequences.length;
            newSequences = new Sequence[oldSize - numToRemove];

            for (int i = 0, pos = 0; i < oldSize; i++) {
                final Sequence testSequence = oldSequences[i];
                if (sequence != testSequence) {
                    newSequences[pos++] = testSequence;
                }
            }
        }
        while (!sequenceUpdater.compareAndSet(holder, oldSequences, newSequences));

        return numToRemove != 0;
    }

    private static <T> int countMatching(T[] values, final T toMatch) {
        int numToRemove = 0;
        for (T value : values) {
            if (value == toMatch) {
                numToRemove++;
            }
        }
        return numToRemove;
    }
}
