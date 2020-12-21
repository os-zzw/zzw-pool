package com.github.zzw;

/**
 * @author zhangzhewei
 */
public interface ThrowableSupplier<R, E extends Throwable> {

    R get() throws E;

}
