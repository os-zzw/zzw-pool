package zzw;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import zzw.impl.KeyPoolExecutorBuilder;

/**
 * @author zhangzhewei
 */
public interface KeyPoolExecutor<K> extends KeyPool<K, ListeningExecutorService> {

    int DEFAULT_QUEUE_SIZE = 100;

    static KeyPoolExecutorBuilder newKeyAffinityExecutor() {
        return new KeyPoolExecutorBuilder();
    }

    static <K> KeyPoolExecutor<K> newSerializingExecutor(int parallelism, String threadName) {
        return newSerializingExecutor(parallelism, DEFAULT_QUEUE_SIZE, threadName);
    }

    static <K> KeyPoolExecutor<K> newSerializingExecutor(int parallelism, int queueBufferSize,
            String threadName) {
        return newKeyAffinityExecutor().count(parallelism)
                .executor(new Supplier<ExecutorService>() {

                    private final ThreadFactory threadFactory = new ThreadFactoryBuilder() //
                            .setNameFormat(threadName) //
                            .build();

                    @Override
                    public ExecutorService get() {
                        LinkedBlockingQueue<Runnable> queue;
                        if (queueBufferSize > 0) {
                            queue = new LinkedBlockingQueue<Runnable>(queueBufferSize) {

                                @Override
                                public boolean offer(Runnable runnable) {
                                    try {
                                        put(runnable);
                                        return true;
                                    } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                    }
                                    return false;
                                }
                            };
                        } else {
                            queue = new LinkedBlockingQueue<>();
                        }
                        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, queue,
                                threadFactory);
                    }
                })//
                .build();
    }

    default <T> ListenableFuture<T> submit(K key, Callable<T> task) {
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

    default void execute(K key, Runnable runnable) {
        submit(key, () -> {
            runnable.run();
            return null;
        });
    }

}
