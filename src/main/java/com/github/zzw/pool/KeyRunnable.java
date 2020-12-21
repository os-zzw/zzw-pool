package com.github.zzw.pool;

/**
 * @author zhangzhewei
 */
public interface KeyRunnable<T> extends Runnable {

    T getKey();

}
