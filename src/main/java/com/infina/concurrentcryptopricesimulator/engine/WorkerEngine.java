package com.infina.concurrentcryptopricesimulator.engine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class WorkerEngine {

    private final int workerCount;
    private final ExecutorService executor;

    public WorkerEngine(int workerCount) {
        if (workerCount < 1) {
            throw new IllegalArgumentException("workerCount must be >= 1");
        }
        this.workerCount = workerCount;
        this.executor = Executors.newFixedThreadPool(workerCount, new WorkerThreadFactory());
    }

    public int workerCount() {
        return workerCount;
    }

    public ExecutorService executor() {
        return executor;
    }


    public void startWorkers(
            BlockingQueue<PriceWorker.StubTask> queue,
            CountDownLatch completionLatch
    ) {
        for (int i = 0; i < workerCount; i++) {
            executor.submit(new PriceWorker(queue, completionLatch));
        }
    }
}
