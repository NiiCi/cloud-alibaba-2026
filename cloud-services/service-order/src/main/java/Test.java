import com.alibaba.nacos.shaded.com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public class Test {

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException, ExecutionException {
        CompletableFuture<String> future1 = new CompletableFuture<>();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(2,
                10,
                1000, TimeUnit.HOURS,
                new LinkedBlockingDeque<>(), new ThreadPoolExecutor.AbortPolicy());
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("22222");
        executor.execute(() -> {
            System.out.println(threadLocal.get());
            System.out.println("111111");
        });

        /**
         * ForkJoinPool
         */
        /*Thread thread1 = new Thread(() -> {
            System.out.println("thread1 running");
        });

        Thread thread2 = new Thread(() -> {
            try {
                thread1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("thread2 running");
        });

        Thread thread3 = new Thread(() -> {
            try {
                thread2.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("thread3 running");
        });

        thread1.start();
        thread2.start();
        thread3.start();*/


        /**
         * countDownLatch
         */
        /*CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        CountDownLatch latch3 = new CountDownLatch(1);

        Thread latchThread1 = new Thread(() -> {
            System.out.println("latch1 running");
            latch1.countDown();
        });

        Thread latchThread2 = new Thread(() -> {
            System.out.println("latch2 running");
            latch2.countDown();
        });

        Thread latchThread3 = new Thread(() -> {
            System.out.println("latch3 running");
            latch3.countDown();
        });

        latchThread1.start();
        latch1.await();

        latchThread2.start();
        latch2.await();

        latchThread3.start();
        latch3.await();*/

        /**
         * CyclicBarrier
         */
        /*CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
        Thread barrier1 = new Thread(() -> {
            System.out.println("barrier1 running");
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        });

        Thread barrier2 = new Thread(() -> {
            System.out.println("barrier2 running");
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        });

        Thread barrier3 = new Thread(() -> {
            System.out.println("barrier3 running");
            try {
                cyclicBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        });

        barrier1.start();
        cyclicBarrier.await();

        barrier2.start();
        cyclicBarrier.await();

        barrier3.start();
        cyclicBarrier.await();*/

        /**
         * CompletableFuture
         */
        List<String> list = Lists.newArrayList();
        InheritableThreadLocal<String> local = new InheritableThreadLocal<>();
        local.set("2222");
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("thread1 running " + local.get());
            list.add("T1");
            return list;
        }, executor).thenApplyAsync(res -> {
            System.out.println("thread1 已启动 " + local.get());
            list.add("success");
            throw new RuntimeException("异常测试");
            //return list;
        }, executor);

        try {
            List<String> result = future.get();
            System.out.println(result);
        } catch (Exception e) {
            System.out.println("异常: " + e.getMessage());
        }


        /*future.handle((res, exception) -> {
            if (Objects.nonNull(exception)) {
                System.out.println("异常: " + exception.getMessage());
            }
            return null;
        });*/

        System.out.println("main running");


    }
}
