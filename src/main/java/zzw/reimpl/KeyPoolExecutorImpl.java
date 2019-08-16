package zzw.reimpl;

import java.util.function.Supplier;

import com.google.common.util.concurrent.ListeningExecutorService;

import zzw.KeyPool;
import zzw.KeyPoolExecutor;
import zzw.impl.LazyKeyPool;

/**
 * @author zhangzhewei
 * Created on 2019-08-16
 */
public class KeyPoolExecutorImpl<K> extends LazyKeyPool<K, ListeningExecutorService> implements
                                KeyPoolExecutor<K> {

    public KeyPoolExecutorImpl(Supplier<KeyPool<K, ListeningExecutorService>> poolFactory) {
        super(poolFactory);
    }

    public KeyPoolExecutor<K> serializingExecutor() {
        return null;
    }
}
