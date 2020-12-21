package com.github.zzw.pool.impl;

import static java.util.Objects.hash;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.github.zzw.pool.KeyRunnable;
import com.github.zzw.pool.ThrowablePredicate;
import com.github.zzw.pool.ThrowableSupplier;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.Uninterruptibles;


/**
 * @author zhangzhewei
 */
@SuppressWarnings("unchecked")
public class QueuePool<T> implements Iterable<QueueProxy> {

    private final List<QueueProxy> proxyList;
    private final BlockingQueue<QueueProxy> unBindProxys;

    public QueuePool(Supplier<BlockingQueue<Runnable>> queueSupplier, int queueCount) {
        proxyList = new ArrayList<>(queueCount);
        for (int i = 0; i < queueCount; i++) {
            proxyList.add(new QueueProxy(i, queueSupplier.get()));
        }
        this.unBindProxys = new LinkedBlockingQueue<>();
    }

    public Runnable take() throws InterruptedException {
        QueueProxy proxy = bindQueue(unBindProxys::take);
        Runnable runnable = catchingGetRunnable(() -> proxy.queue().take(), proxy);
        return buildKeyRunnable(runnable, proxy);
    }

    public Runnable peek() {
        QueueProxy proxy = unBindProxys.peek();
        if (proxy == null) {
            return null;
        }
        return proxy.queue().peek();
    }

    public boolean remove(KeyRunnable<T> runnable) {
        QueueProxy proxy = selectQueue(getKey(runnable));
        return proxy.queue().remove(runnable);
    }

    public Runnable poll() {
        QueueProxy proxy = bindQueue(unBindProxys::poll);
        if (proxy == null) {
            return null;
        }
        Runnable runnable = proxy.queue().poll();
        return buildKeyRunnable(runnable, proxy);
    }

    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        QueueProxy proxy = bindQueue(unBindProxys::poll);
        if (proxy == null) {
            return null;
        }
        Runnable runnable = catchingGetRunnable(() -> proxy.queue().poll(timeout, unit), proxy);
        return buildKeyRunnable(runnable, proxy);
    }

    public boolean offer(Runnable runnable, long timeout, TimeUnit unit) throws InterruptedException {
        return addRunnable(runnable, proxy -> proxy.queue().offer(runnable, timeout, unit));
    }

    public boolean offer(Runnable runnable) {
        return addRunnable(runnable, proxy -> proxy.queue().offer(runnable));
    }

    public void put(Runnable runnable) throws InterruptedException {
        addRunnable(runnable, proxy -> {
            proxy.queue().put(runnable);
            return true;
        });
    }

    public int remainingCapacity() {
        return (int) proxyList.stream().mapToLong(proxy -> proxy.queue().remainingCapacity()).sum();
    }

    public int drainTo(Collection<? super Runnable> c) {
        return proxyList.stream().mapToInt(proxy -> proxy.queue().drainTo(c)).sum();
    }

    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        int left = maxElements;
        for (QueueProxy proxy : proxyList) {
            int count = proxy.queue().drainTo(c, left);
            left = left - count;
            if (left <= 0) {
                break;
            }
        }
        return maxElements - left;
    }

    private QueueProxy selectQueue(T key) {
        return proxyList.get(hash(key) % proxyList.size());
    }

    private void unbindProxy(QueueProxy proxy) {
        //先解绑
        proxy.unbind();
        //随后加入到可被线程绑定队列中
        //如果有task再入队
        if (proxy.isNotEmpty()) {
            putUnbindProxy(proxy);
        } else {
            //如果没有task当下次task进来的时候再入队
            proxy.getCanInQueue().compareAndSet(false, true);
            //double check
            if (proxy.isNotEmpty() && proxy.getCanInQueue().compareAndSet(true, false)) {
                putUnbindProxy(proxy);
            }
        }
    }

    private <E extends Throwable> boolean addRunnable(Runnable runnable, ThrowablePredicate<QueueProxy, E> predicate) throws E {
        QueueProxy proxy = selectQueue(getKey(runnable));
        if (!predicate.test(proxy)) {
            return false;
        }
        //每次添加task检测是否在队列中或已经被绑定
        if (proxy.getCanInQueue().compareAndSet(true, false)) {
            putUnbindProxy(proxy);
        }
        return true;
    }

    private T getKey(Runnable runnable) {
        return ((KeyRunnable<T>) runnable).getKey();
    }

    private Runnable buildKeyRunnable(Runnable runnable, QueueProxy proxy) {
        return () -> {
            try {
                runnable.run();
            } finally {
                //执行完成后解绑当前线程.
                //防止线程和当前队列一直绑定导致其他队列无法被消费
                unbindProxy(proxy);
            }
        };
    }

    private <E extends InterruptedException> Runnable catchingGetRunnable(ThrowableSupplier<Runnable, E> queueSupplier, QueueProxy proxy)
            throws InterruptedException {
        try {
            return queueSupplier.get();
        } catch (InterruptedException e) {
            unbindProxy(proxy);
            throw e;
        }
    }

    private <E extends Throwable> QueueProxy bindQueue(ThrowableSupplier<QueueProxy, E> queueSupplier) throws E {
        retry:
        while (true) {
            QueueProxy proxy = queueSupplier.get();
            if (proxy == null) {
                return null;
            }
            while (true) {
                if (!proxy.bind()) {
                    continue retry;
                }
                if (proxy.isNotEmpty()) {
                    return proxy;
                } else {
                    proxy.unbind();
                }
                //如果这时候有了task重新绑定
                if (proxy.isNotEmpty()) {
                    continue;
                }
                //最终还是空task就取下一个proxy
                continue retry;
            }
        }
    }

    private void putUnbindProxy(QueueProxy proxy) {
        Uninterruptibles.putUninterruptibly(unBindProxys, proxy);
    }

    @Override
    public Iterator<QueueProxy> iterator() {
        return proxyList.iterator();
    }

    public Iterator<Runnable> elementIterator() {
        return Iterators.concat(proxyList.stream().map(QueueProxy::queue).map(Collection::iterator).toArray(Iterator[]::new));
    }

    public int size() {
        return proxyList.stream().map(QueueProxy::queue).mapToInt(BlockingQueue::size).sum();
    }
}
