package com.infina.concurrentcryptopricesimulator.api.dto;

public record CoinComparisonResponseDto(
    String id,
    long initial,
    long expected,
    long unsafe,
    long safe,
    long expectedUpdateCount,
    long unsafeUpdateCount,
    long safeUpdateCount
){}
