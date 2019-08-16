package zzw;

import java.util.concurrent.TimeUnit;

import org.junit.Test;


/**
 * Unit test for simple App.
 */
public class AppTest {

    private static final int THREAD_COUNT = 4;

    private final KeyPoolExecutor<Long> worker = KeyPoolExecutor
            .newSerializingExecutor(THREAD_COUNT, "zzw-pool");

    @Test
    public void testPool() throws InterruptedException {
        worker.execute(1L, () -> {
            System.out.println(Thread.currentThread().getId());
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("end");
        });
        worker.execute(2L, () -> {
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
