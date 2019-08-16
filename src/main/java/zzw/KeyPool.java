package zzw;

/**
 * @author zhangzhewei
 * Created on 2019-08-14
 */
public interface KeyPool<K, V> extends AutoCloseable {

    V select(K key);

    void finishCall(K key);

}
