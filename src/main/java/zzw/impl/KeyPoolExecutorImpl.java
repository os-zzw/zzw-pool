package zzw.impl;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import zzw.KeyPool;
import zzw.KeyPoolExecutor;

/**
 * @author zhangzhewei
 * Created on 2019-08-16
 */
public class KeyPoolExecutorImpl<K> extends LazyKeyPool<K, ListeningExecutorService> implements
                                KeyPoolExecutor<K> {

    public KeyPoolExecutorImpl(Supplier<KeyPool<K, ListeningExecutorService>> poolFactory) {
        super(poolFactory);
    }

    @Override
    public <T> ListenableFuture<T> submit(K key, Callable<T> task) {
        ListeningExecutorService service = select(key);
        boolean addCallback = false;
        ListenableFuture<T> future = service.submit(task);
        try {
            Futures.addCallback(future, new FutureCallback<T>() {

                @Override
                public void onSuccess(T t) {
                    finishCall(key);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    finishCall(key);
                }
            }, directExecutor());
            addCallback = true;
            return future;
        } finally {
            if (!addCallback) {
                finishCall(key);
            }
        }
    }

}
