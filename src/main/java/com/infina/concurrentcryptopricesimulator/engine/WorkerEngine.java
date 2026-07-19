package com.infina.concurrentcryptopricesimulator.engine;

import java.time.Duration;
import java.util.Objects;
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
            throw new IllegalArgumentException("workerCount en az 1 olmalıdır.");
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
            CountDownLatch completionLatch,
            PriceTaskProcessor processor
    ) {
        for (int i = 0; i < workerCount; i++) {
            executor.submit(new PriceWorker(queue, completionLatch, processor));
        }
    }

    public boolean awaitCompletion(
            CountDownLatch completionLatch,
            Duration timeout
    ) throws InterruptedException {
        Objects.requireNonNull(completionLatch, "completionLatch null olamaz.");
        Objects.requireNonNull(timeout, "timeout null olamaz.");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout negatif olamaz.");
        }

        return completionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
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
