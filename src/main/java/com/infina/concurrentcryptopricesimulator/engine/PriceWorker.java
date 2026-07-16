package com.infina.concurrentcryptopricesimulator.engine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;


public final class PriceWorker implements Runnable {


     //todo PriceUpdateTask record geliştirmesi tamamlanınca silinecek

    public record StubTask(long sequence, String coinId, long delta) {

        public static StubTask poisonPill() {
            return new StubTask(-1L, "STOP", 0L);
        }

        public boolean isPoisonPill() {
            return sequence < 0;
        }
    }

    private final BlockingQueue<StubTask> queue;
    private final CountDownLatch completionLatch;

    public PriceWorker(BlockingQueue<StubTask> queue, CountDownLatch completionLatch) {
        this.queue = queue;
        this.completionLatch = completionLatch;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                StubTask task = queue.take();
                if (task.isPoisonPill()) {
                    break;
                }
                try {
                  //todo coinState güncellemesi çağırılacak
                } finally {
                    completionLatch.countDown();
                }
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
