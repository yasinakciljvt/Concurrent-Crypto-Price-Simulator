package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;

import java.util.concurrent.CountDownLatch;


public final class PriceWorker implements Runnable {

    private final TaskQueue queue;
    private final CountDownLatch completionLatch;
    private final PriceTaskProcessor processor;

    public PriceWorker(
            TaskQueue queue,
            CountDownLatch completionLatch,
            PriceTaskProcessor processor
    ) {
        this.queue = queue;
        this.completionLatch = completionLatch;
        this.processor = processor;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                PriceUpdateTask task = queue.take();
                if (TaskQueue.isPoisonPill(task)) {
                    break;
                }
                try {
                    processor.process(task);
                } finally {
                    completionLatch.countDown();
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
