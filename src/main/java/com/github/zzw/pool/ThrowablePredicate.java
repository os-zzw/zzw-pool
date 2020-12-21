package com.github.zzw.pool;

/**
 * @author zhangzhewei
 */
public interface ThrowablePredicate<T, X extends Throwable> {
    boolean test(T t) throws X;
}
