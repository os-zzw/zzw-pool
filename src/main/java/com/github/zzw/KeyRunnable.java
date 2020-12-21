package com.github.zzw;

/**
 * @author zhangzhewei
 */
public interface KeyRunnable<T> extends Runnable {

    T getKey();

}
