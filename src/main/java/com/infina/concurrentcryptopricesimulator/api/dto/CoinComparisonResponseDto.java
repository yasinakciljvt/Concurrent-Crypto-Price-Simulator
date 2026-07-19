package com.infina.concurrentcryptopricesimulator.api.dto;

import com.infina.concurrentcryptopricesimulator.simulation.CoinComparison;

public record CoinComparisonResponseDto(
        String id,
        long initial,
        long expected,
        long unsafe,
        long safe,
        long expectedUpdateCount,
        long unsafeUpdateCount,
        long safeUpdateCount
) {
    public static CoinComparisonResponseDto from(CoinComparison comparison) {
        return new CoinComparisonResponseDto(
                comparison.id(),
                comparison.initialPrice(),
                comparison.expectedPrice(),
                comparison.unsafePrice(),
                comparison.safePrice(),
                comparison.expectedUpdateCount(),
                comparison.unsafeUpdateCount(),
                comparison.safeUpdateCount()
        );
    }
}
