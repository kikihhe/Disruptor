package com.xiaohe.common;

import com.xiaohe.provider.Cursored;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * @author : 小何
 * @Description : 管理序号组的类，提供了对一组序号进行操作的功能
 * @date : 2024-02-05 20:52
 */
public class SequenceGroups {
    /**
     * 线程安全的添加 Sequence 到一个Sequence数组中。使用CAS保证安全
     * @param holder 拥有原先序号组的类，从中取出老序号组，再将新序号组添加进去
     * @param updater 老序号组的线程安全的操作器
     * @param cursor 用于获取生产者进度，将新添加进来的序号都更新为这个进度
     * @param sequencesToadd 需要添加的序号
     * @param <T>
     */
    public static <T> void addSequences(final T holder,
                                        final AtomicReferenceFieldUpdater<T, Sequence[]> updater,
                                        final Cursored cursor,
                                        final Sequence... sequencesToadd) {
        // 当前生产者的进度
        long cursorSequence;
        // 更新后的序号组
        Sequence[] updatedSequences;
        // 更新前的序号组
        Sequence[] currentSequences;
        do {
            currentSequences = updater.get(holder);
            // 将原来的序号数组放到updatedSequences中
            updatedSequences = Arrays.copyOf(currentSequences, currentSequences.length + sequencesToadd.length);
            cursorSequence = cursor.getCursor();
            // 刷新新来的这些序号的进度，然后将它们放进数组
            int index = currentSequences.length;
            for (Sequence sequence : sequencesToadd) {
                sequence.set(cursorSequence);
                updatedSequences[index++] = sequence;
            }
            // 使用CAS将更新好的数组放入holder中
        } while (!updater.compareAndSet(holder, currentSequences, updatedSequences));
    }

    /**
     * 在序号数组中删除指定的序号
     * @param holder 序号数组
     * @param updater 原子修改器
     * @param sequence 需要被删除的序号
     * @return sequence是否存在于该数组
     * @param <T>
     */
    public static <T> boolean removeSequence(final T holder,
                                             final AtomicReferenceFieldUpdater<T, Sequence[]> updater,
                                             final Sequence sequence) {
        // 需要删除的个数
        int numToRemove;
        // 删除前的数组
        Sequence[] oldSequences;
        // 删除后的数组
        Sequence[] newSequences;
        do {
            oldSequences = updater.get(holder);
            numToRemove = countMatching(oldSequences, sequence);
            if (0 == numToRemove) {
                return false;
            }
            final int oldSize = oldSequences.length;
            newSequences = new Sequence[oldSize - numToRemove];
            for (int i = 0, pos = 0; i < oldSize; i++) {
                Sequence testSequence = oldSequences[i];
                if (sequence != testSequence) {
                    newSequences[pos++] = testSequence;
                }
            }
        } while (!updater.compareAndSet(holder, oldSequences, newSequences));
        return numToRemove != 0;
    }

    /**
     * 查看一个元素在数组中出现了多少次
     * @param values 数组
     * @param toMatch 元素
     * @return
     * @param <T>
     */
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
