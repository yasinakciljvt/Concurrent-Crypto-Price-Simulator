package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerCompletionTest {

    @Test
    void returnsTrueWhenAllTasksComplete() throws Exception {
        int workers = 2;
        TaskQueue queue = new TaskQueue();
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger processedTasks = new AtomicInteger();
        WorkerEngine engine = new WorkerEngine(workers, Duration.ofSeconds(2));

        engine.startWorkers(queue, latch, task -> processedTasks.incrementAndGet());
        queue.put(new PriceUpdateTask(1, "BTC", 10));
        queue.put(new PriceUpdateTask(2, "ETH", -5));
        queue.putPoisonPills(workers);

        assertTrue(engine.awaitCompletion(latch, Duration.ofSeconds(2)));
        assertEquals(2, processedTasks.get());
        assertTrue(engine.shutdownGracefully());
    }

    @Test
    void returnsFalseWhenTasksDoNotCompleteBeforeTimeout() throws Exception {
        TaskQueue queue = new TaskQueue();
        CountDownLatch latch = new CountDownLatch(1);
        WorkerEngine engine = new WorkerEngine(1, Duration.ofSeconds(2));

        engine.startWorkers(queue, latch, task -> {
        });

        assertFalse(engine.awaitCompletion(latch, Duration.ofMillis(100)));

        queue.putPoisonPills(1);
        assertTrue(engine.shutdownGracefully());
    }
}
