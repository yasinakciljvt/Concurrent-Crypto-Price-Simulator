package com.infina.concurrentcryptopricesimulator.service;

import com.infina.concurrentcryptopricesimulator.api.dto.CoinResponseDto;
import com.infina.concurrentcryptopricesimulator.api.dto.SimulationStatsResponseDto;
import com.infina.concurrentcryptopricesimulator.exception.NoSimulationYetException;
import com.infina.concurrentcryptopricesimulator.exception.SimulationAlreadyRunningException;
import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import com.infina.concurrentcryptopricesimulator.simulation.SimulationEngine;
import com.infina.concurrentcryptopricesimulator.simulation.SimulationReport;
import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SimulationService {

	private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

	private final SimulationEngine simulationEngine;
	private final AtomicBoolean simulationRunning = new AtomicBoolean(false);
	private volatile SimulationStatsResponseDto lastStats;
	private volatile List<CoinResponseDto> lastCoins;

	public SimulationService(SimulationEngine simulationEngine,
							 InMemoryCoinRepository<SafeCoinState> safeCoinRepository) {
		this.simulationEngine = simulationEngine;
		this.lastCoins = toCoinResponses(safeCoinRepository.findAllSnapshots());
	}

	public List<CoinResponseDto> getCoins() {
		synchronized (this) {
			return lastCoins;
		}
	}

	public SimulationStatsResponseDto getStats() {
		if (lastStats == null) {
			throw new NoSimulationYetException();
		}
		return lastStats;
	}

	public SimulationStatsResponseDto simulate(int updates, int workers, Long seed) {
		if (!simulationRunning.compareAndSet(false, true)) {
			throw new SimulationAlreadyRunningException();
		}

		try {
			SimulationReport report = simulationEngine.runFullSimulation(updates, workers, seed);
			SimulationStatsResponseDto stats = SimulationStatsResponseDto.from(report);

			lastCoins = toCoinResponses(report.safeCoinSnapshots());
			lastStats = stats;

			logSummary(stats);
			return stats;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Simülasyon kesildi (interrupted)", e);
		} finally {
			simulationRunning.set(false);
		}
	}

	private void logSummary(SimulationStatsResponseDto stats) {
		log.info("Simulation completed: updates={}, workers={}, seed={}, "
						+ "safeElapsedMs={}, safeThroughput={} task/sec, "
						+ "unsafeElapsedMs={}, unsafeThroughput={} task/sec, "
						+ "unsafeProcessed={}, safeProcessed={}, safeInvariantPassed={}",
				stats.submittedUpdates(), stats.workers(), stats.seed(),
				stats.safeElapsedMs(), Math.round(stats.safeThroughputPerSec()),
				stats.unsafeElapsedMs(), Math.round(stats.unsafeThroughputPerSec()),
				stats.unsafeProcessedUpdates(), stats.safeProcessedUpdates(),
				stats.safeInvariantPassed());
	}

	private static List<CoinResponseDto> toCoinResponses(List<CoinSnapshot> snapshots) {
		return snapshots.stream()
				.map(CoinResponseDto::from)
				.toList();
	}
}
