package com.github.zzw;


import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.github.zzw.pool.impl.KeyExecutor;
import com.github.zzw.pool.utils.KeyExecutorUtils;


/**
 * @author zhangzhewei
 * Created on 2020-12-17
 */
public class BaseTest {

    /**
     * 针对于同一个key顺序执行的线程池
     */
    @Test
    public void testKeyPool() throws InterruptedException {
        KeyExecutor keyExecutor = KeyExecutorUtils.newKeySerializingExecutor(() -> 4);
        Map<String, Integer> map = new HashMap<>();
        String str1 = "1";
        String str2 = "2";
        //线程1对key:1添加操作
        keyExecutor.execute(str1, () -> {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            map.put(str1, 100);
            System.out.println("线程1end");
        });
        //线程2对key:1删除操作
        keyExecutor.execute(str1, () -> {
            map.remove(str1);
            System.out.println("线程2end");
        });
        //线程3对key:2增加操作
        keyExecutor.execute(2, () -> {
            map.put(str2, 100);
            System.out.println("线程3end");
        });

        Thread.sleep(1300);
    }
}
