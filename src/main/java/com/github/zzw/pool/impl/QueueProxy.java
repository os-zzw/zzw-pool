package com.github.zzw.pool.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author zhangzhewei
 */
public class QueueProxy {

    private final int queueId;
    private final BlockingQueue<Runnable> queue;
    private final AtomicReference<Thread> thread = new AtomicReference<>();
    private final AtomicBoolean canInQueue = new AtomicBoolean(true);

    public QueueProxy(int queueId, BlockingQueue<Runnable> queue) {
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
     * 解绑线程
     */
    public void unbind() {
        thread.compareAndSet(Thread.currentThread(), null);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public boolean isNotEmpty() {
        return !queue.isEmpty();
    }

    /**
     * 获取该代理是否已经加入了线程绑定队列
     */
    public AtomicBoolean getCanInQueue() {
        return canInQueue;
    }

}
