package com.github.zzw;

/**
 * @author zhangzhewei
 */
public interface ThrowablePredicate<T, X extends Throwable> {
    boolean test(T t) throws X;
}
