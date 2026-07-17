package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;

import java.util.concurrent.CountDownLatch;


public final class PriceWorker implements Runnable {

    private final TaskQueue queue;
    private final CountDownLatch completionLatch;

    public PriceWorker(TaskQueue queue, CountDownLatch completionLatch) {
        this.queue = queue;
        this.completionLatch = completionLatch;
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
                    // TODO: processor.process(task) eklenecek
                } finally {
                    completionLatch.countDown();
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
