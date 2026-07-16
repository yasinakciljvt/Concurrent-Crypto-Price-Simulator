package com.infina.concurrentcryptopricesimulator.model;

public record CoinSnapshot(
        String id,
        long initialPrice,
        long currentPrice,
        long updateCount,
        long lastDelta,
        String lastUpdatedBy
) {
}
