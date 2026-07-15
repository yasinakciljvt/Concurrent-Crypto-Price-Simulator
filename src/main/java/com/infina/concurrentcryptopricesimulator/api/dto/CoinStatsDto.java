package com.infina.concurrentcryptopricesimulator.api.dto;

public record CoinStatsDto (
    String id,
    long initial,
    long expected,
    long unsafe,
    long safe,
    long expectedUpdateCount,
    long unsafeUpdateCount,
    long safeUpdateCount
){}
