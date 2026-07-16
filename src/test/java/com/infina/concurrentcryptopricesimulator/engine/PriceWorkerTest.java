package com.infina.concurrentcryptopricesimulator.engine;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceWorkerTest {

    @Test
    void consumesAllTasksThenStopsOnPoisonPill() throws Exception {
        LinkedBlockingQueue<PriceWorker.StubTask> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(3);

        Thread worker = new Thread(new PriceWorker(queue, latch), "worker-1");
        worker.start();

        queue.put(new PriceWorker.StubTask(1, "BTC", 10));
        queue.put(new PriceWorker.StubTask(2, "ETH", -5));
        queue.put(new PriceWorker.StubTask(3, "SOL", 2));
        queue.put(PriceWorker.StubTask.poisonPill());

        assertTrue(latch.await(3, TimeUnit.SECONDS), "all real tasks should be consumed");
        worker.join(3_000);
        assertTrue(!worker.isAlive(), "worker should exit after poison pill");
        assertEquals(0, latch.getCount());
    }

    @Test
    void poisonPillDoesNotCountDownLatch() throws Exception {
        LinkedBlockingQueue<PriceWorker.StubTask> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread worker = new Thread(new PriceWorker(queue, latch), "worker-1");
        worker.start();

        queue.put(new PriceWorker.StubTask(1, "BTC", 1));
        queue.put(PriceWorker.StubTask.poisonPill());

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        worker.join(3_000);
        assertEquals(0, latch.getCount());
    }
}
