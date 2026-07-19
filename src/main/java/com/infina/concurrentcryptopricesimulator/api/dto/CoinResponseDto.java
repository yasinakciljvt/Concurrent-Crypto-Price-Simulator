package com.infina.concurrentcryptopricesimulator.api.dto;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;

public record CoinResponseDto(
        String id,
        long initialPrice,
        long currentPrice,
        long updateCount,
        long lastDelta,
        String lastUpdatedBy
) {
    public static CoinResponseDto from(CoinSnapshot coinSnapshot) {
        return new CoinResponseDto(
                coinSnapshot.id(),
                coinSnapshot.initialPrice(),
                coinSnapshot.currentPrice(),
                coinSnapshot.updateCount(),
                coinSnapshot.lastDelta(),
                coinSnapshot.lastUpdatedBy()
        );
    }
}
