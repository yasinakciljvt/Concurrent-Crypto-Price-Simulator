package com.infina.concurrentcryptopricesimulator.api.dto;

import java.util.List;

public record StatsDto (
    long seed,
    int submittedUpdates,
    long unsafeProcessUpdates,
    long safeProcessedUpdates,
    int workers,
    long unsafeElapsedMs,
    long safeElapsedMs,
    long unsafeThroughPerSec,
    long safeThroughPerSec,
    long safeInvariantPassed,
    List<CoinStatsDto> coins
){}
