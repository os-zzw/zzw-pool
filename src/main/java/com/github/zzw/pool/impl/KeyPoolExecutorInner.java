package com.github.zzw.pool.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * @author zhangzhewei
 */
@SuppressWarnings("unchecked")
public class KeyPoolExecutorInner<T> extends ThreadPoolExecutor {

    private volatile boolean hasInitCoreThreads = false;

    public KeyPoolExecutorInner(IntSupplier poolSizeSupplier, Supplier<BlockingQueue<Runnable>> queueSupplier, IntSupplier queueCountSupplier) {
        super(poolSizeSupplier.getAsInt(), poolSizeSupplier.getAsInt(), 0, TimeUnit.MILLISECONDS,
                new KeyBlockingQueue<T>(queueSupplier, queueCountSupplier));
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        if (!hasInitCoreThreads) {
            hasInitCoreThreads = true;
            prestartAllCoreThreads();
        }
        if (!isShutdown() && getQueue().offer(command)) {
            if (isShutdown() && getQueue().remove(command)) {
                rejectTask(command);
            }
        } else {
            rejectTask(command);
        }
    }

    private void rejectTask(Runnable command) {
        getRejectedExecutionHandler().rejectedExecution(command, this);
    }
}

