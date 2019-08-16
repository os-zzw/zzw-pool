package zzw.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import com.google.common.util.concurrent.ListeningExecutorService;

import zzw.KeyPool;
import zzw.KeyPoolExecutor;

/**
 * @author zhangzhewei
 */
public class KeyPoolExecutorBuilder {

    private final KeyPoolBuilder<ListeningExecutorService> builder = new KeyPoolBuilder<>();

    public <K> KeyPoolExecutor<K> build() {
        builder.ensure();
        return new ExecutorImpl<>(builder::buildInner);
    }

    public KeyPoolExecutorBuilder count(int count) {
        builder.count(count);
        return this;
    }

    public KeyPoolExecutorBuilder executor(Supplier<ExecutorService> factory) {
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

    private class ExecutorImpl<K> extends LazyKeyPool<K, ListeningExecutorService> implements
                              KeyPoolExecutor<K> {

        ExecutorImpl(Supplier<KeyPool<K, ListeningExecutorService>> factory) {
            super(factory);
        }
    }

}
