package zzw.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Comparator.comparingInt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.phantomthief.util.ThrowableConsumer;

import zzw.KeyPool;

/**
 * @author zhangzhewei
 */
public class KeyPoolImpl<K, V> implements KeyPool<K, V> {

    private final List<PoolRef> all;
    private final ThrowableConsumer<V, Exception> deposeFunc;
    private final Map<K, PoolRef> map = new ConcurrentHashMap<>();
    private boolean useRandom;

    public KeyPoolImpl(Supplier<V> factory, int poolCount,
            ThrowableConsumer<V, Exception> deposeFunc, boolean useRandom) {
        this.useRandom = useRandom;
        this.deposeFunc = checkNotNull(deposeFunc);
        this.all = IntStream.range(0, poolCount)//
                .mapToObj(it -> factory.get())//
                .map(PoolRef::new)//
                .collect(Collectors.toList());
    }

    @Override
    public V select(K key) {
        PoolRef poolRef = map.compute(key, (k, v) -> {
            if (v == null) {
                if (useRandom) {
                    v = all.get(ThreadLocalRandom.current().nextInt(all.size()));
                } else {
                    v = all.stream()//
                            .min(comparingInt(PoolRef::concurrency))//
                            .orElseThrow(IllegalStateException::new);
                }
            }
            v.incrConcurrency();
            return v;
        });
        return poolRef.ref();
    }

    @Override
    public void finishCall(K key) {
        map.computeIfPresent(key, (k, v) -> {
            if (v.decrConcurrency()) {
                return null;
            } else {
                return v;
            }
        });
    }

    @Override
    public void close() throws Exception {
        synchronized (all) {
            while (all.stream().anyMatch(valueRef -> valueRef.concurrency() > 0)) {
                all.wait();
            }
        }
        for (PoolRef valueRef : all) {
            deposeFunc.accept(valueRef.value);
        }
    }

    private class PoolRef {

        private final V value;
        private final AtomicInteger concurrency = new AtomicInteger();

        public PoolRef(V value) {
            this.value = value;
        }

        void incrConcurrency() {
            this.concurrency.getAndIncrement();
        }

        boolean decrConcurrency() {
            int valueRefConcurrency = concurrency.decrementAndGet();
            if (valueRefConcurrency <= 0) {
                synchronized (all) {
                    all.notifyAll();
                }
            }
            return valueRefConcurrency <= 0;
        }

        V ref() {
            return value;
        }

        int concurrency() {
            return concurrency.get();
        }
    }

}
