package zzw;

import java.util.concurrent.Callable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * @author zhangzhewei
 */
public interface KeyPoolExecutor<K> extends KeyPool<K, ListeningExecutorService> {

    <T> ListenableFuture<T> submit(K key, Callable<T> task);

    default void execute(K key, Runnable runnable) {
        submit(key, () -> {
            runnable.run();
            return null;
        });
    }

}
