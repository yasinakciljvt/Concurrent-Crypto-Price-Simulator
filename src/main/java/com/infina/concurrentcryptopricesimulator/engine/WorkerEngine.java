package com.infina.concurrentcryptopricesimulator.engine;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public final class WorkerEngine {

    private final int workerCount;
    private final Duration terminationTimeout;
    private final ExecutorService executor;
    private final AtomicBoolean shutDown = new AtomicBoolean(false);

    public WorkerEngine(int workerCount) {
        this(workerCount, Duration.ofSeconds(30));
    }

    public WorkerEngine(int workerCount, Duration terminationTimeout) {
        if (workerCount < 1) {
            throw new IllegalArgumentException("workerCount must be >= 1");
        }
        this.workerCount = workerCount;
        this.terminationTimeout = terminationTimeout;
        this.executor = Executors.newFixedThreadPool(workerCount, new WorkerThreadFactory());
    }

    public int workerCount() {
        return workerCount;
    }

    public ExecutorService executor() {
        return executor;
    }

    public void startWorkers(
            TaskQueue queue,
            CountDownLatch completionLatch
    ) {
        for (int i = 0; i < workerCount; i++) {
            executor.submit(new PriceWorker(queue, completionLatch));
        }
    }


    public boolean shutdownGracefully() throws InterruptedException {
        if (!shutDown.compareAndSet(false, true)) {
            return executor.isTerminated();
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(terminationTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                return executor.awaitTermination(terminationTimeout.toMillis(), TimeUnit.MILLISECONDS);
            }
            return true;
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw interrupted;
        }
    }
}
