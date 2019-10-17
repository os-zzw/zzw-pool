package zzw;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import zzw.impl.KeyPoolExecutorBuilder;

/**
 * Unit test for simple App.
 */
public class AppTest {

    private static final int THREAD_COUNT = 4;

    private final KeyPoolExecutor<Long> worker = KeyPoolExecutorBuilder
            .newSerializingExecutor(THREAD_COUNT, "zzw-pool");

    /**
     * 针对于同一个key顺序执行的线程池
     */
    @Test
    public void testKeyPool() throws InterruptedException {
        worker.execute(1L, () -> {
            System.out.println(Thread.currentThread().getId());
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("end");
        });
        worker.execute(1L, () -> {
            System.out.println(Thread.currentThread().getId());
        });
        Thread.sleep(5000L);
    }

    @Test
    public void tt() {
        int photoAuthorId = (int) TimeUnit.DAYS.toSeconds(90);
        System.out.println(photoAuthorId);
    }
}
