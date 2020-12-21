package com.github.zzw.pool.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntSupplier;

import com.github.zzw.pool.KeyRunnable;

/**
 * @author zhangzhewei
 */
public class KeyPoolExecutor<T> {

    private final KeyPoolExecutorInner keyPoolExecutorInner;

    /**
     * @param parallelCount 消费不同key并发数
     * @param queueBufferCount 每个缓存队列最大缓存元素个数
     */
    public KeyPoolExecutor(IntSupplier parallelCount, int queueBufferCount) {
        this.keyPoolExecutorInner = new KeyPoolExecutorInner<T>(parallelCount, () -> new LinkedBlockingQueue<>(queueBufferCount), parallelCount);
    }

    public KeyPoolExecutor(IntSupplier threadCountSupplier, IntSupplier queueCountSupplier, int queueBufferCount) {
        this.keyPoolExecutorInner =
                new KeyPoolExecutorInner<T>(threadCountSupplier, () -> new LinkedBlockingQueue<>(queueBufferCount), queueCountSupplier);
    }

    /**
     *
     * @param key 顺序消费的key
     * @param runnable task
     */
    public void execute(T key, Runnable runnable) {
        keyPoolExecutorInner.execute(new KeyRunnable<T>() {
            @Override
            public T getKey() {
                return key;
            }

            @Override
            public void run() {
                runnable.run();
            }
        });
    }

}
