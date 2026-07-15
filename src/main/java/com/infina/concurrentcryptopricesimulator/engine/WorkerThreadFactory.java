package com.infina.concurrentcryptopricesimulator.engine;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public final class WorkerThreadFactory implements ThreadFactory {

    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "worker-" + sequence.getAndIncrement());
        thread.setDaemon(false);
        return thread;
    }
}
