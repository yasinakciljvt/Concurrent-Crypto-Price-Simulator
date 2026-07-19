package com.infina.concurrentcryptopricesimulator.api.dto;

import com.infina.concurrentcryptopricesimulator.simulation.SimulationReport;

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

	public static SimulationStatsResponseDto from(SimulationReport report) {
		List<CoinComparisonResponseDto> coins = report.coinComparisons().stream()
				.map(CoinComparisonResponseDto::from)
				.toList();

		return new SimulationStatsResponseDto(
				report.seed(),
				report.submittedUpdates(),
				report.workers(),
				report.unsafeStats().getActualValue(),
				report.safeStats().getActualValue(),
				report.unsafeStats().getDurationMs(),
				report.safeStats().getDurationMs(),
				report.unsafeThroughputPerSec(),
				report.safeThroughputPerSec(),
				report.safeInvariantValid(),
				coins
		);
	}
}
