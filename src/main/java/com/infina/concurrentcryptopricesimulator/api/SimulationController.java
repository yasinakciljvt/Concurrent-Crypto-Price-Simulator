package com.infina.concurrentcryptopricesimulator.api;

import com.infina.concurrentcryptopricesimulator.api.dto.CoinResponseDto;
import com.infina.concurrentcryptopricesimulator.api.dto.ErrorResponseDto;
import com.infina.concurrentcryptopricesimulator.api.dto.SimulationStatsResponseDto;
import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import com.infina.concurrentcryptopricesimulator.simulation.SimulationEngine;
import com.infina.concurrentcryptopricesimulator.simulation.SimulationReport;
import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Validated
@RestController
@Tag(
		name = "Simulation",
		description = "Eşzamanlı kripto fiyat simülasyonu: çalıştırma, güvenli coin durumu ve karşılaştırmalı istatistikler"
)
public class SimulationController {

	private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

	private final SimulationEngine simulationEngine;
	private final AtomicBoolean simulationRunning = new AtomicBoolean(false);
	private volatile SimulationStatsResponseDto lastStats;
	private volatile List<CoinResponseDto> lastCoins;

	public SimulationController(SimulationEngine simulationEngine,
								InMemoryCoinRepository<SafeCoinState> safeCoinRepository) {
		this.simulationEngine = simulationEngine;
		this.lastCoins = toCoinResponses(safeCoinRepository.findAllSnapshots());
	}

	@GetMapping("/coins")
	@Operation(
			summary = "Coin durumlarini listeler",
			description = "Son tamamlanan simulasyondaki guvenli (safe) coin durumunu dondurur. "
					+ "Henuz simulasyon calismadiysa coin'lerin baslangic degerleri doner."
	)
	@ApiResponse(responseCode = "200", description = "Guvenli coin durumu")
	public List<CoinResponseDto> getCoins() {
		return lastCoins;
	}

	@GetMapping("/stats")
	@Operation(
			summary = "Simulasyon istatistiklerini dondurur",
			description = "Son tamamlanan simulasyonun sure, throughput, islenen gorev sayisi ve coin bazli "
					+ "beklenen/guvensiz/guvenli karsilastirmasini dondurur."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Son simulasyonun istatistikleri"),
			@ApiResponse(responseCode = "404", description = "Henuz hic simulasyon calistirilmadi",
					content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
	})
	public SimulationStatsResponseDto getStats() {
		if (lastStats == null) {
			throw new NoSimulationYetException();
		}
		return lastStats;
	}

	@PostMapping("/simulate")
	@Operation(
			summary = "Simulasyonu calistirir",
			description = "Coin durumlarini sifirlar, verilen seed ile immutable gorev listesini uretir, "
					+ "beklenen sonucu tek thread'de hesaplar, ardindan AYNI listeyle once guvensiz sonra "
					+ "guvenli simulasyonu calistirir. Butun gorevler bitmeden cevap donmez."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Simulasyon tamamlandi"),
			@ApiResponse(responseCode = "400", description = "Gecersiz updates/workers parametresi",
					content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
			@ApiResponse(responseCode = "409", description = "Halihazirda calisan bir simulasyon var",
					content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
	})
	public SimulationStatsResponseDto simulate(
			@Parameter(description = "Uretilecek gorev sayisi", example = "10000")
			@RequestParam @Min(1) @Max(100_000) int updates,
			@Parameter(description = "Worker thread sayisi", example = "4")
			@RequestParam @Min(1) @Max(16) int workers,
			@Parameter(description = "Tekrarlanabilir gorev uretimi icin seed. Verilmezse uretilir ve cevapta doner.",
					example = "42")
			@RequestParam(required = false) Long seed
	) {
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
