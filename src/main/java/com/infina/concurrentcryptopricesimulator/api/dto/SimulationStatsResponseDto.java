package com.infina.concurrentcryptopricesimulator.api.dto;

import java.util.List;

public record SimulationStatsResponseDto(
		long seed,
		int submittedUpdates,
		int workers,
		long unsafeProcessedUpdates,
		long safeProcessedUpdates,
		long unsafeElapsedMs,
		long safeElapsedMs,
		double unsafeThroughputPerSec,
		double safeThroughputPerSec,
		boolean safeInvariantPassed,
		List<CoinComparisonResponseDto> coins
) {
}