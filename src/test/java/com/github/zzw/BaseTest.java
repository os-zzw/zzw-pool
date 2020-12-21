package com.github.zzw;


import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.github.zzw.impl.KeyExecutor;
import com.github.zzw.utils.KeyExecutorUtils;


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
        Map<Integer, Integer> map = new HashMap<>();

        //线程1对key:1添加操作
        keyExecutor.execute(1, () -> {
            try {
                Thread.sleep(1000L);
                map.put(1, 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("线程1end");
        });
        //线程2对key:1删除操作
        keyExecutor.execute(1, () -> {
            map.remove(1);
            System.out.println("线程2end");
        });
        //线程3对key:2增加操作
        keyExecutor.execute(2, () -> {
            map.put(2, 100);
            System.out.println("线程3end");
        });

        Thread.sleep(1300);
    }
}
