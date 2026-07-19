package com.infina.concurrentcryptopricesimulator.simulation;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;

import java.util.List;

public record SimulationReport(
        long seed,
        int submittedUpdates,
        int workers,
        Stats safeStats,
        Stats unsafeStats,
        List<CoinComparison> coinComparisons,
        List<CoinSnapshot> safeCoinSnapshots,
        boolean safeInvariantValid,
        boolean unsafeInvariantValid
) {

    public double safeThroughputPerSec() {
        return throughput(safeStats);
    }

    public double unsafeThroughputPerSec() {
        return throughput(unsafeStats);
    }

    private static double throughput(Stats stats) {
        if (stats.getDurationNanos() == 0) {
            return 0;
        }
        return (stats.getActualValue() * 1_000_000_000.0) / stats.getDurationNanos();
    }
}
