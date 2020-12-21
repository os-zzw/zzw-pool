package com.github.zzw.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zhangzhewei
 */
public class QueueHolder {

    private final int queueId;
    private final BlockingQueue<Runnable> queue;
    private final AtomicReference<Thread> thread = new AtomicReference<>();
    private final AtomicBoolean idle = new AtomicBoolean(true);//闲置状态不能被

    public QueueHolder(int queueId, BlockingQueue<Runnable> queue) {
        this.queueId = queueId;
        this.queue = queue;
    }

    public int getQueueId() {
        return queueId;
    }

    public BlockingQueue<Runnable> queue() {
        return queue;
    }

    /**
     * cas方式绑定线程
     */
    public boolean bind() {
        return thread.compareAndSet(null, Thread.currentThread());
    }

    /**
     * 解绑线程并返回当前holder
     */
    public void unbind() {
        thread.compareAndSet(Thread.currentThread(), null);
    }

    public AtomicBoolean getIdle() {
        return idle;
    }
}
