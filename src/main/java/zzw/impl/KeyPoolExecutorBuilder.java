package zzw.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import zzw.KeyPoolExecutor;

/**
 * @author zhangzhewei
 */
public class KeyPoolExecutorBuilder {

    private static final int DEFAULT_QUEUE_SIZE = 100;

    private final KeyPoolBuilder<ListeningExecutorService> builder = new KeyPoolBuilder<>();

    public static <K> KeyPoolExecutor<K> newSerializingExecutor(int parallelism,
            String threadName) {
        return newSerializingExecutor(parallelism, DEFAULT_QUEUE_SIZE, threadName);
    }

    public static <K> KeyPoolExecutor<K> newSerializingExecutor(int parallelism,
            int queueBufferSize, String threadName) {
        KeyPoolExecutorBuilder builder = new KeyPoolExecutorBuilder();
        return builder.poolCount(parallelism).poolFactory(new Supplier<ExecutorService>() {

            private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                    .setNameFormat(threadName).build();

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
                return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, queue, threadFactory);
            }
        }).build();
    }

    public <K> KeyPoolExecutor<K> build() {
        builder.ensure();
        return new KeyPoolExecutorImpl<>(builder::buildInner);
    }

    public KeyPoolExecutorBuilder poolCount(int count) {
        builder.poolCount(count);
        return this;
    }

    public KeyPoolExecutorBuilder poolFactory(Supplier<ExecutorService> factory) {
        checkNotNull(factory);
        builder.factoty(() -> {
            ExecutorService executorService = factory.get();
            if (executorService instanceof ListeningExecutorService) {
                return (ListeningExecutorService) executorService;
            } else {
                return listeningDecorator(executorService);
            }
        });
        return this;
    }
}
