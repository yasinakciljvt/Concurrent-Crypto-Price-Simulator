package com.infina.concurrentcryptopricesimulator.simulation;

import com.infina.concurrentcryptopricesimulator.counter.Counter;
import com.infina.concurrentcryptopricesimulator.engine.WorkerEngine;

public class SimulationEngine {

    private final int threadCount;
    private final int incrementsPerThread;

    public SimulationEngine(int threadCount, int incrementsPerThread) {
        this.threadCount = threadCount;
        this.incrementsPerThread = incrementsPerThread;
    }

    public long runSimulation(Counter counter) throws InterruptedException {
        WorkerEngine workerEngine = new WorkerEngine(threadCount);

        for (int i = 0; i < threadCount; i++) {
            workerEngine.executor().submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
            });
        }

        workerEngine.shutdownGracefully();

        return counter.getValue();
    }
}