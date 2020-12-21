package com.github.zzw.pool.utils;

import java.util.function.IntSupplier;

import com.github.zzw.pool.impl.KeyPoolExecutor;

/**
 * @author zhangzhewei
 * Created on 2020-12-21
 */
public class KeyExecutorUtils {

    private static final int DEFAULT_QUEUE_BUFFER_COUNT = 1000;

    public static <T> KeyPoolExecutor<T> newKeySerializingExecutor(IntSupplier parallelCount, int queueBufferCount) {
        return new KeyPoolExecutor<>(parallelCount, queueBufferCount);
    }

    public static <T> KeyPoolExecutor<T> newKeySerializingExecutor(IntSupplier parallelCount) {
        return new KeyPoolExecutor<>(parallelCount, DEFAULT_QUEUE_BUFFER_COUNT);
    }
}
