package com.github.zzw.pool.utils;

import java.util.function.IntSupplier;

import com.github.zzw.pool.impl.KeyExecutor;

/**
 * @author zhangzhewei
 * Created on 2020-12-21
 */
public class KeyExecutorUtils {

    private static final int DEFAULT_QUEUE_BUFFER_COUNT = 1000;

    public static KeyExecutor newKeySerializingExecutor(IntSupplier parallelCount, int queueBufferCount) {
        return new KeyExecutor(parallelCount, queueBufferCount);
    }

    public static KeyExecutor newKeySerializingExecutor(IntSupplier parallelCount) {
        return new KeyExecutor(parallelCount, DEFAULT_QUEUE_BUFFER_COUNT);
    }
}
