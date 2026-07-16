package com.infina.concurrentcryptopricesimulator;

import com.infina.concurrentcryptopricesimulator.counter.Counter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulationEngine {
    private final int threadCount;
    private final int incrementsPerThread;

    public SimulationEngine(int threadCount, int incrementsPerThread) {
        this.threadCount = threadCount;
        this.incrementsPerThread = incrementsPerThread;
    }

    public long runSimulation(Counter counter) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        return counter.getValue();
    }
}