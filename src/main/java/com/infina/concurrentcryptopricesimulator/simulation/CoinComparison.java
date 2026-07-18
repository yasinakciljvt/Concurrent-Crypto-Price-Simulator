package com.infina.concurrentcryptopricesimulator.simulation;

public record CoinComparison(
        String id,
        long initialPrice,
        long expectedPrice,
        long unsafePrice,
        long safePrice,
        long expectedUpdateCount,
        long unsafeUpdateCount,
        long safeUpdateCount
) {
}
