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
    private int count;
    private ThrowableConsumer<V, Exception> depose;
    private Boolean usingRandom;

    <K> KeyPool<K, V> buildInner() {
        return new KeyPoolImpl<>(factory, count, depose, usingRandom);
    }

    void ensure() {
        if (count < 0) {
            throw new IllegalArgumentException();
        }
        if (depose == null) {
            depose = it -> {};
        }
        if (usingRandom == null) {
            usingRandom = count > THREAD_COUNT;
        }
    }

    public <T extends KeyPoolBuilder<V>> T factoty(Supplier<V> factory) {
        this.factory = factory;
        return (T) this;
    }

    public <T extends KeyPoolBuilder<V>> T count(int count) {
        this.count = count;
        return (T) this;
    }

}
