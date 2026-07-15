package com.infina.concurrentcryptopricesimulator.engine;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerShutdownTest {

    @Test
    void gracefulShutdownTerminatesAfterPoisonPills() throws Exception {
        int workers = 2;
        WorkerEngine engine = new WorkerEngine(workers, Duration.ofSeconds(5));
        LinkedBlockingQueue<PriceWorker.StubTask> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(2);

        engine.startWorkers(queue, latch);

        queue.put(new PriceWorker.StubTask(1, "BTC", 10));
        queue.put(new PriceWorker.StubTask(2, "ETH", -5));
        for (int i = 0; i < workers; i++) {
            queue.put(PriceWorker.StubTask.poisonPill());
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(engine.shutdownGracefully());
        assertTrue(engine.executor().isTerminated());
    }

    @Test
    void timeoutTriggersShutdownNowWhenWorkersBlockedOnTake() throws Exception {
        WorkerEngine engine = new WorkerEngine(2, Duration.ofMillis(200));
        LinkedBlockingQueue<PriceWorker.StubTask> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(0);

        engine.startWorkers(queue, latch);

        assertTrue(engine.shutdownGracefully());
        assertTrue(engine.executor().isTerminated());
    }

    @Test
    void secondShutdownIsIdempotent() throws Exception {
        WorkerEngine engine = new WorkerEngine(1, Duration.ofSeconds(2));
        LinkedBlockingQueue<PriceWorker.StubTask> queue = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(0);

        engine.startWorkers(queue, latch);
        queue.put(PriceWorker.StubTask.poisonPill());

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertTrue(engine.shutdownGracefully());
        assertTrue(engine.shutdownGracefully());
        assertTrue(engine.executor().isTerminated());
    }
}
