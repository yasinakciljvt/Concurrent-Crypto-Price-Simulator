package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerEngineTest {

    private WorkerEngine engine;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (engine != null) {
            engine.shutdownGracefully();
        }
    }

    @Test
    void fixedPoolProcessesTasksWithNamedWorkers() throws Exception {
        int workers = 4;
        int taskCount = 20;

        engine = new WorkerEngine(workers);
        TaskQueue queue = new TaskQueue();
        Set<String> workerNames = ConcurrentHashMap.newKeySet();

        CountDownLatch namedLatch = new CountDownLatch(taskCount) {
            @Override
            public void countDown() {
                workerNames.add(Thread.currentThread().getName());
                super.countDown();
            }
        };

        engine.startWorkers(queue, namedLatch, task -> {
        });

        for (int i = 1; i <= taskCount; i++) {
            queue.put(new PriceUpdateTask(i, "BTC", 1));
        }
        queue.putPoisonPills(workers);

        assertTrue(namedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(workers, engine.workerCount());
        assertFalse(workerNames.isEmpty());
        assertTrue(workerNames.size() <= workers);

        for (String name : workerNames) {
            assertTrue(name.matches("worker-[1-4]"), "unexpected worker: " + name);
        }
        assertTrue(engine.shutdownGracefully());
        engine = null; // AfterEach tekrar kapatmasın diye
    }
}
