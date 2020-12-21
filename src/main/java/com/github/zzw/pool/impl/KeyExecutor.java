package com.github.zzw.pool.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntSupplier;

import com.github.zzw.pool.KeyRunnable;

/**
 * @author zhangzhewei
 * Created on 2020-12-19
 */
public class KeyExecutor<T> {

    private final KeyPoolExecutor keyPoolExecutor;

    /**
     *
     * @param parallelCount 消费不同key并发数
     * @param queueBufferCount 每个缓存队列最大缓存元素个数
     */
    public KeyExecutor(IntSupplier parallelCount, int queueBufferCount) {
        this.keyPoolExecutor = new KeyPoolExecutor(parallelCount, () -> new LinkedBlockingQueue<>(queueBufferCount), parallelCount);
    }

    /**
     *
     * @param threadCountSupplier
     * @param queueCountSupplier
     * @param queueBufferCount
     */
    public KeyExecutor(IntSupplier threadCountSupplier, IntSupplier queueCountSupplier, int queueBufferCount) {
        this.keyPoolExecutor = new KeyPoolExecutor(threadCountSupplier, () -> new LinkedBlockingQueue<>(queueBufferCount), queueCountSupplier);
    }

    public void execute(T id, Runnable runnable) {
        keyPoolExecutor.execute(new KeyRunnable() {
            @Override
            public T getKey() {
                return id;
            }

            @Override
            public void run() {
                runnable.run();
            }
        });
    }

}
