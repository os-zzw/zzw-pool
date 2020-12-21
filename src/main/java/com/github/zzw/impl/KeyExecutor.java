package com.github.zzw.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntSupplier;

import com.github.zzw.KeyRunnable;

/**
 * @author zhangzhewei
 * Created on 2020-12-19
 */
public class KeyExecutor<T> {

    private final KeyPoolExecutor keyPoolExecutor;

    public KeyExecutor(IntSupplier parallelCount, int queueBufferCount) {
        this.keyPoolExecutor = new KeyPoolExecutor(parallelCount, () -> new LinkedBlockingQueue<>(queueBufferCount), parallelCount);
    }

    public KeyExecutor(KeyPoolExecutor keyPoolExecutor) {
        this.keyPoolExecutor = keyPoolExecutor;
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
