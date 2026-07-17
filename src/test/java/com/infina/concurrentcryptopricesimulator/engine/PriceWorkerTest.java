package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceWorkerTest {

    @Test
    void consumesAllTasksThenStopsOnPoisonPill() throws Exception {
        TaskQueue queue = new TaskQueue();
        CountDownLatch latch = new CountDownLatch(3);

        Thread worker = new Thread(new PriceWorker(queue, latch), "worker-1");
        worker.start();

        queue.put(new PriceUpdateTask(1, "BTC", 10));
        queue.put(new PriceUpdateTask(2, "ETH", -5));
        queue.put(new PriceUpdateTask(3, "SOL", 2));
        queue.putPoisonPills(1);

        assertTrue(latch.await(3, TimeUnit.SECONDS), "all real tasks should be consumed");
        worker.join(3_000);
        assertTrue(!worker.isAlive(), "worker should exit after poison pill");
        assertEquals(0, latch.getCount());
    }

    @Test
    void poisonPillDoesNotCountDownLatch() throws Exception {
        TaskQueue queue = new TaskQueue();
        CountDownLatch latch = new CountDownLatch(1);

        Thread worker = new Thread(new PriceWorker(queue, latch), "worker-1");
        worker.start();

        queue.put(new PriceUpdateTask(1, "BTC", 1));
        queue.putPoisonPills(1);

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        worker.join(3_000);
        assertEquals(0, latch.getCount());
    }
}
