package com.github.zzw.impl;

import static com.google.common.base.Suppliers.memoize;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.github.zzw.KeyRunnable;


/**
 * @author zhangzhewei
 */
public class KeyBlockingQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {

    private final Supplier<BlockingQueue<Runnable>> queueSupplier;
    private final IntSupplier queueCountSupplier;
    private final Supplier<QueuePool> queuePoolSupplier;

    public KeyBlockingQueue(Supplier<BlockingQueue<Runnable>> queueSupplier, IntSupplier queueCountSupplier) {
        this.queueSupplier = queueSupplier;
        this.queueCountSupplier = queueCountSupplier;
        this.queuePoolSupplier = memoize(() -> new QueuePool(queueSupplier, queueCountSupplier.getAsInt()));
    }

    private QueuePool queuePool() {
        return queuePoolSupplier.get();
    }

    @Override
    public Iterator<Runnable> iterator() {
        return queuePool().elementIterator();
    }

    @Override
    public int size() {
        return queuePool().size();
    }

    @Override
    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        return queuePool().offer(runnable, timeout, unit);
    }

    @Override
    public boolean offer(Runnable runnable) {
        return queuePool().offer(runnable);
    }

    @Override
    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queuePool().poll(timeout, unit);
    }

    @Override
    public Runnable poll() {
        return queuePool().poll();
    }

    @Override
    public Runnable peek() {
        return queuePool().peek();
    }

    @Override
    public void put(Runnable runnable) throws InterruptedException {
        queuePool().put(runnable);
    }

    @Override
    public Runnable take() throws InterruptedException {
        return queuePool().take();
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof KeyRunnable) {
            return queuePool().remove((KeyRunnable) o);
        }
        return false;
    }

    @Override
    public int remainingCapacity() {
        return queuePool().remainingCapacity();
    }

    @Override
    public int drainTo(Collection<? super Runnable> c) {
        return queuePool().drainTo(c);
    }

    @Override
    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        return queuePool().drainTo(c, maxElements);
    }


}
