package zzw.impl;

import java.util.function.Supplier;

import javax.annotation.concurrent.NotThreadSafe;

import com.github.phantomthief.util.ThrowableConsumer;

import zzw.KeyPool;

/**
 * @author zhangzhewei
 */
@SuppressWarnings({ "CheckStyle", "unchecked" })
@NotThreadSafe
public class KeyPoolBuilder<V> {

    private static final int THREAD_COUNT = 20;

    private Supplier<V> factory;
    private int poolCount;
    private ThrowableConsumer<V, Exception> depose;
    private Boolean usingRandom;

    public <K> KeyPool<K, V> buildInner() {
        return new KeyPoolImpl<>(factory, poolCount, depose, usingRandom);
    }

    public void ensure() {
        if (poolCount < 0) {
            throw new IllegalArgumentException();
        }
        if (depose == null) {
            depose = it -> {};
        }
        if (usingRandom == null) {
            usingRandom = poolCount > THREAD_COUNT;
        }
        if (factory == null) {
            throw new IllegalArgumentException();
        }
    }

    public <T extends KeyPoolBuilder<V>> T factoty(Supplier<V> factory) {
        this.factory = factory;
        return (T) this;
    }

    public <T extends KeyPoolBuilder<V>> T poolCount(int count) {
        this.poolCount = count;
        return (T) this;
    }

}
