package com.infina.concurrentcryptopricesimulator.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
        LinkedBlockingQueue<PriceWorker.StubTask> queue = new LinkedBlockingQueue<>();
        Set<String> workerNames = ConcurrentHashMap.newKeySet();

        CountDownLatch namedLatch = new CountDownLatch(taskCount) {
            @Override
            public void countDown() {
                workerNames.add(Thread.currentThread().getName());
                super.countDown();
            }
        };

        engine.startWorkers(queue, namedLatch);

        for (int i = 1; i <= taskCount; i++) {
            queue.put(new PriceWorker.StubTask(i, "BTC", 1));
        }
        for (int i = 0; i < workers; i++) {
            queue.put(PriceWorker.StubTask.poisonPill());
        }

        assertTrue(namedLatch.await(5, TimeUnit.SECONDS));
        assertEquals(workers, workerNames.size());
        for (int i = 1; i <= workers; i++) {
            assertTrue(workerNames.contains("worker-" + i), "missing worker-" + i);
        }
        assertTrue(engine.shutdownGracefully());
        engine = null; // AfterEach tekrar kapatmasın diye
    }
}
