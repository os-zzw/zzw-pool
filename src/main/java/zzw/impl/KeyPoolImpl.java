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

    private final List<ValueRef> all;
    private final ThrowableConsumer<V, Exception> deposeFunc;
    private final Map<K, KeyRef> map = new ConcurrentHashMap<>();
    private boolean useRandom;

    public KeyPoolImpl(Supplier<V> factory, int poolCount,
            ThrowableConsumer<V, Exception> deposeFunc, boolean useRandom) {
        this.useRandom = useRandom;
        this.deposeFunc = checkNotNull(deposeFunc);
        this.all = IntStream.range(0, poolCount)//
                .mapToObj(it -> factory.get())//
                .map(ValueRef::new)//
                .collect(Collectors.toList());
    }

    @Override
    public V select(K key) {
        KeyRef keyRef = map.compute(key, (k, v) -> {
            if (v == null) {
                if (useRandom) {
                    v = new KeyRef(all.get(ThreadLocalRandom.current().nextInt(all.size())));
                } else {
                    v = all.stream().min(comparingInt(ValueRef::getConcurrency))//
                            .map(KeyRef::new)//
                            .orElseThrow(IllegalStateException::new);
                }
            }
            v.incrConcurrency();
            return v;
        });
        return keyRef.ref();
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
            while (all.stream().anyMatch(valueRef -> valueRef.getConcurrency() > 0)) {
                all.wait();
            }
        }
        for (ValueRef valueRef : all) {
            deposeFunc.accept(valueRef.value);
        }
    }

    private class KeyRef {

        private final ValueRef valueRef;
        private final AtomicInteger concurrency = new AtomicInteger();

        public KeyRef(ValueRef valueRef) {
            this.valueRef = valueRef;
        }

        void incrConcurrency() {
            this.concurrency.getAndIncrement();
            valueRef.concurrency.getAndIncrement();
        }

        boolean decrConcurrency() {
            int r = concurrency.decrementAndGet();
            int valueRefConcurrency = concurrency.decrementAndGet();
            if (valueRefConcurrency <= 0) {
                synchronized (all) {
                    all.notifyAll();
                }
            }
            return r <= 0;
        }

        V ref() {
            return valueRef.value;
        }
    }

    private class ValueRef {

        private final V value;
        private final AtomicInteger concurrency = new AtomicInteger();

        public ValueRef(V value) {
            this.value = value;
        }

        int getConcurrency() {
            return concurrency.get();
        }
    }
}
