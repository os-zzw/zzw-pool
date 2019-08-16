package zzw.impl;

import static com.github.phantomthief.util.MoreSuppliers.lazy;

import java.util.function.Supplier;

import com.github.phantomthief.util.MoreSuppliers.CloseableSupplier;

import zzw.KeyPool;

/**
 * @author zhangzhewei
 * Created on 2019-08-14
 */
public class LazyKeyPool<K, V> implements KeyPool<K, V> {

    private final CloseableSupplier<KeyPool<K, V>> factory;

    public LazyKeyPool(Supplier<KeyPool<K, V>> poolFactory) {
        this.factory = lazy(poolFactory, false);
    }

    @Override
    public V select(K key) {
        return factory.get().select(key);
    }

    @Override
    public void finishCall(K key) {
        factory.get().finishCall(key);
    }

    @Override
    public void close() throws Exception {
        factory.tryClose(KeyPool::close);
    }

}
