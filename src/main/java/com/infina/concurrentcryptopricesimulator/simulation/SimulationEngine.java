package com.infina.concurrentcryptopricesimulator.simulation;

import com.infina.concurrentcryptopricesimulator.counter.Counter;
import com.infina.concurrentcryptopricesimulator.counter.SafeCounter;
import com.infina.concurrentcryptopricesimulator.counter.UnsafeCounter;
import com.infina.concurrentcryptopricesimulator.engine.WorkerEngine;

public class SimulationEngine {

    private final int threadCount;
    private final int incrementsPerThread;
    private final ExpectedResultCalculator expectedResultCalculator;
    private final InvariantChecker invariantChecker;

    public SimulationEngine(int threadCount, int incrementsPerThread) {
        this.threadCount = threadCount;
        this.incrementsPerThread = incrementsPerThread;
        this.expectedResultCalculator = new ExpectedResultCalculator();
        this.invariantChecker = new InvariantChecker();
    }

    /**
     * Hem Safe hem de Unsafe sayaçları koşturarak karşılaştırmalı tam simülasyon raporu üretir.
     */
    public SimulationReport runFullSimulation() throws InterruptedException {
        // 1. Sayaç nesnelerini oluştur
        Counter safeCounter = new SafeCounter();
        Counter unsafeCounter = new UnsafeCounter();

        // 2. Simülasyonları sırayla çalıştır ve metriklerini topla
        Stats safeStats = runSimulation(safeCounter);
        Stats unsafeStats = runSimulation(unsafeCounter);

        // 3. Invariant (Değişmez Kural) kontrollerini gerçekleştir
        boolean isSafeValid = invariantChecker.checkSafeCounterInvariant(
                safeStats.getActualValue(), safeStats.getExpectedValue()
        );
        boolean isUnsafeValid = invariantChecker.checkUnsafeCounterInvariant(
                unsafeStats.getActualValue(), unsafeStats.getExpectedValue()
        );

        // 4. API katmanına beslenecek toplu raporu döndür
        return new SimulationReport(safeStats, unsafeStats, isSafeValid, isUnsafeValid);
    }

    /**
     * Verilen sayaç implementasyonu üzerinde eşzamanlı artırım işlemini yürütür.
     */
    public Stats runSimulation(Counter counter) throws InterruptedException {
        long expectedValue = expectedResultCalculator.calculateExpectedResult(threadCount, incrementsPerThread);
        long startTime = System.currentTimeMillis();

        WorkerEngine workerEngine = new WorkerEngine(threadCount);

        for (int i = 0; i < threadCount; i++) {
            workerEngine.executor().submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
            });
        }

        workerEngine.shutdownGracefully();

        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        return new Stats(expectedValue, counter.getValue(), durationMs);
    }

    /**
     * Yasin'in endpoint katmanına (Controller) JSON olarak dönecek veri transfer nesnesi (DTO).
     */
    public static class SimulationReport {
        private final Stats safeStats;
        private final Stats unsafeStats;
        private final boolean safeInvariantValid;
        private final boolean unsafeInvariantValid;

        public SimulationReport(Stats safeStats, Stats unsafeStats, boolean safeInvariantValid, boolean unsafeInvariantValid) {
            this.safeStats = safeStats;
            this.unsafeStats = unsafeStats;
            this.safeInvariantValid = safeInvariantValid;
            this.unsafeInvariantValid = unsafeInvariantValid;
        }

        public Stats getSafeStats() { return safeStats; }
        public Stats getUnsafeStats() { return unsafeStats; }
        public boolean isSafeInvariantValid() { return safeInvariantValid; }
        public boolean isUnsafeInvariantValid() { return unsafeInvariantValid; }
    }
}