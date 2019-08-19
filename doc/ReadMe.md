
### 使用场景

#### KeyPoolExecutor newSerializingExecutor
    适用于异步消费任务,当需要对具体某一个key进行顺序消费的时候

#### 使用举例

```
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
        worker.execute(2L, () -> {
            System.out.println(Thread.currentThread().getId());
        });
        Thread.sleep(5000L);
    }
```
