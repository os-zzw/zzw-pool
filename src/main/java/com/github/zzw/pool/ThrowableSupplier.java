package com.github.zzw.pool;

/**
 * @author zhangzhewei
 */
public interface ThrowableSupplier<R, E extends Throwable> {

    R get() throws E;

}
