package com.github.zzw.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.zzw.KeyRunnable;
import com.github.zzw.KeySupplier;
import com.github.zzw.ThrowablePredicate;
import com.github.zzw.ThrowableSupplier;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.Uninterruptibles;


/**
 * @author zhangzhewei
 */
public class QueuePool implements Iterable<QueueHolder> {

    private final List<QueueHolder> holderList;
    private final BlockingQueue<QueueHolder> unBindHolders;

    public QueuePool(Supplier<BlockingQueue<Runnable>> queueSupplier, int queueCount) {
        holderList = new ArrayList<>(queueCount);
        for (int i = 0; i < queueCount; i++) {
            holderList.add(new QueueHolder(i, queueSupplier.get()));
        }
        this.unBindHolders = new LinkedBlockingQueue<>();
    }

    public Runnable take() throws InterruptedException {
        QueueHolder holder = bindQueueBlock();
        Runnable runnable = catchingGetRunnable(() -> holder.queue().take(), holder);
        return buildUnBindRunnable(runnable, holder);
    }

    /**
     * 阻塞式绑定一个非空的任务队列
     */
    private QueueHolder bindQueueBlock() throws InterruptedException {
        return doBindQueue(unBindHolders::take);
    }

    private void unbindHolder(QueueHolder holder) {
        holder.unbind();
        putUnbindHolder(holder);
    }

    public Runnable peek() {
        QueueHolder holder = unBindHolders.peek();
        if (holder == null) {
            return null;
        }
        return holder.queue().peek();
    }

    public boolean remove(KeyRunnable runnable) {
        QueueHolder holder = selectQueue(getKey(runnable));
        return holder.queue().remove(runnable);
    }

    public Runnable poll() {
        QueueHolder holder = doBindQueue(unBindHolders::poll);
        if (holder == null) {
            return null;
        }
        Runnable runnable = holder.queue().poll();
        return buildUnBindRunnable(runnable, holder);
    }

    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        QueueHolder holder = doBindQueue(unBindHolders::poll);
        if (holder == null) {
            return null;
        }
        Runnable runnable = catchingGetRunnable(() -> holder.queue().poll(timeout, unit), holder);
        return buildUnBindRunnable(runnable, holder);
    }

    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        return addRunnable(runnable, holder -> holder.queue().offer(runnable, timeout, unit));
    }

    public boolean offer(Runnable runnable) {
        return addRunnable(runnable, holder -> holder.queue().offer(runnable));
    }

    public void put(Runnable runnable) throws InterruptedException {
        addRunnable(runnable, holder -> {
            holder.queue().put(runnable);
            return true;
        });
    }

    public int remainingCapacity() {
        return (int) holderList.stream().mapToLong(holder -> holder.queue().remainingCapacity()).sum();
    }

    public int drainTo(Collection<? super Runnable> c) {
        return holderList.stream().mapToInt(holder -> holder.queue().drainTo(c)).sum();
    }

    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        int left = maxElements;
        for (QueueHolder holder : holderList) {
            int count = holder.queue().drainTo(c, left);
            left = left - count;
            if (left <= 0) {
                break;
            }
        }
        return maxElements - left;
    }

    private QueueHolder selectQueue(long key) {
        return holderList.get((int) (key % holderList.size()));
    }

    private <T extends Throwable> boolean addRunnable(Runnable runnable, ThrowablePredicate<QueueHolder, T> predicate) throws T {
        QueueHolder queue = selectQueue(getKey(runnable));
        if (!predicate.test(queue)) {
            return false;
        }
        //队列中有任务之后,加入未绑定队列中,等待线程绑定
        putUnbindHolder(queue);
        return true;
    }

    private long getKey(Runnable runnable) {
        return ((KeySupplier) runnable).getKey();
    }

    private Runnable buildUnBindRunnable(Runnable runnable, QueueHolder holder) {
        return () -> {
            try {
                runnable.run();
            } finally {
                unbindHolder(holder);
            }
        };
    }

    private <E extends InterruptedException> Runnable catchingGetRunnable(ThrowableSupplier<Runnable, E> queueSupplier, QueueHolder holder)
            throws InterruptedException {
        try {
            return queueSupplier.get();
        } catch (InterruptedException e) {
            unbindHolder(holder);
            throw e;
        }
    }

    private <T extends Throwable> QueueHolder doBindQueue(ThrowableSupplier<QueueHolder, T> queueSupplier) throws T {
        retry:
        while (true) {
            QueueHolder holder = queueSupplier.get();
            if (holder == null) {
                return null;
            }
            while (true) {
                if (!holder.bind()) {
                    continue retry;
                }
                if (isNotEmptyHolder(holder)) {
                    return holder;
                } else {
                    holder.unbind();
                }
                //如果解绑后队列非空重新绑定
                if (isNotEmptyHolder(holder)) {
                    continue;
                }
                continue retry;
            }
        }
    }

    private void putUnbindHolder(QueueHolder queue) {
        Uninterruptibles.putUninterruptibly(unBindHolders, queue);
    }

    private boolean isNotEmptyHolder(QueueHolder queue) {
        return !queue.queue().isEmpty();
    }

    @Override
    public Iterator<QueueHolder> iterator() {
        return holderList.iterator();
    }

    public Iterator<Runnable> elementIterator() {
        return Iterators.concat(holderList.stream().map(QueueHolder::queue).map(Collection::iterator).toArray(Iterator[]::new));
    }

    public int size() {
        return holderList.stream().map(QueueHolder::queue).mapToInt(BlockingQueue::size).sum();
    }
}
