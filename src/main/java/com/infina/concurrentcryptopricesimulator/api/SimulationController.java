package com.infina.concurrentcryptopricesimulator.api;

import com.infina.concurrentcryptopricesimulator.api.dto.CoinComparisonResponseDto;
import com.infina.concurrentcryptopricesimulator.api.dto.CoinResponseDto;
import com.infina.concurrentcryptopricesimulator.api.dto.SimulationStatsResponseDto;
import com.infina.concurrentcryptopricesimulator.engine.PriceTaskProcessor;
import com.infina.concurrentcryptopricesimulator.engine.TaskFeeder;
import com.infina.concurrentcryptopricesimulator.engine.TaskProducer;
import com.infina.concurrentcryptopricesimulator.engine.TaskQueue;
import com.infina.concurrentcryptopricesimulator.engine.WorkerEngine;
import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import com.infina.concurrentcryptopricesimulator.repository.DefaultCoinRepositories;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import com.infina.concurrentcryptopricesimulator.state.UnsafeCoinState;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Validated
@RestController
public class SimulationController {

	private static final Duration WORKER_TIMEOUT = Duration.ofSeconds(30);

	private final InMemoryCoinRepository<SafeCoinState> safeCoinRepository;
	private final TaskProducer taskProducer;
	private final AtomicBoolean simulationRunning = new AtomicBoolean(false);
	private volatile SimulationStatsResponseDto lastStats;

	public SimulationController(InMemoryCoinRepository<SafeCoinState> safeCoinRepository,
								TaskProducer taskProducer) {
		this.safeCoinRepository = safeCoinRepository;
		this.taskProducer = taskProducer;
	}

	@GetMapping("/coins")
	public List<CoinResponseDto> getCoins() {
		return safeCoinRepository.findAllSnapshots().stream()
				.map(CoinResponseDto::from)
				.toList();
	}

	@GetMapping("/stats")
	public SimulationStatsResponseDto getStats() {
		if (lastStats == null) {
			throw new NoSimulationYetException();
		}
		return lastStats;
	}

	@PostMapping("/simulate")
	public SimulationStatsResponseDto simulate(
			@RequestParam @Min(1) @Max(100_000) int updates,
			@RequestParam @Min(1) @Max(16) int workers,
			@RequestParam(required = false) Long seed
	) {
		if (!simulationRunning.compareAndSet(false, true)) {
			throw new SimulationAlreadyRunningException();
		}

		try {
			long usedSeed = seed != null ? seed : TaskProducer.randomSeed();
			List<PriceUpdateTask> tasks = taskProducer.generate(updates, usedSeed);

			InMemoryCoinRepository<UnsafeCoinState> expectedRepo = DefaultCoinRepositories.createUnsafe();
			for (PriceUpdateTask task : tasks) {
				expectedRepo.findById(task.coinId()).ifPresent(c -> c.applyDelta(task.delta()));
			}

			InMemoryCoinRepository<UnsafeCoinState> unsafeRepo = DefaultCoinRepositories.createUnsafe();
			long unsafeElapsedMs = runWithWorkers(tasks, workers,
					task -> unsafeRepo.findById(task.coinId()).ifPresent(c -> c.applyDelta(task.delta())));

			safeCoinRepository.resetAll();
			long safeElapsedMs = runWithWorkers(tasks, workers,
					task -> safeCoinRepository.findById(task.coinId()).ifPresent(c -> c.applyDelta(task.delta())));

			SimulationStatsResponseDto stats = buildStatsResponse(
					usedSeed, tasks.size(), workers,
					expectedRepo.findAllSnapshots(),
					unsafeRepo.findAllSnapshots(),
					safeCoinRepository.findAllSnapshots(),
					unsafeElapsedMs, safeElapsedMs
			);
			lastStats = stats;
			return stats;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Simülasyon kesildi (interrupted)", e);
		} finally {
			simulationRunning.set(false);
		}
	}

	private long runWithWorkers(List<PriceUpdateTask> tasks, int workers, PriceTaskProcessor processor)
			throws InterruptedException {
		TaskQueue queue = new TaskQueue();
		CountDownLatch latch = new CountDownLatch(tasks.size());
		WorkerEngine engine = new WorkerEngine(workers);

		long start = System.currentTimeMillis();
		engine.startWorkers(queue, latch, processor);
		Thread feederThread = new TaskFeeder(queue, tasks, workers).start();

		boolean completed = engine.awaitCompletion(latch, WORKER_TIMEOUT);
		feederThread.join();
		engine.shutdownGracefully();

		if (!completed) {
			throw new IllegalStateException("Simülasyon zaman aşımına uğradı (workers=" + workers + ")");
		}
		return System.currentTimeMillis() - start;
	}

	private SimulationStatsResponseDto buildStatsResponse(
			long seed, int submittedUpdates, int workers,
			List<CoinSnapshot> expectedSnapshots,
			List<CoinSnapshot> unsafeSnapshots,
			List<CoinSnapshot> safeSnapshots,
			long unsafeElapsedMs, long safeElapsedMs
	) {
		Map<String, CoinSnapshot> unsafeById = unsafeSnapshots.stream()
				.collect(Collectors.toMap(CoinSnapshot::id, s -> s));
		Map<String, CoinSnapshot> safeById = safeSnapshots.stream()
				.collect(Collectors.toMap(CoinSnapshot::id, s -> s));

		long unsafeProcessedUpdates = unsafeSnapshots.stream().mapToLong(CoinSnapshot::updateCount).sum();
		long safeProcessedUpdates = safeSnapshots.stream().mapToLong(CoinSnapshot::updateCount).sum();

		double unsafeThroughput = unsafeElapsedMs == 0 ? 0 : (unsafeProcessedUpdates * 1000.0) / unsafeElapsedMs;
		double safeThroughput = safeElapsedMs == 0 ? 0 : (safeProcessedUpdates * 1000.0) / safeElapsedMs;

		List<CoinComparisonResponseDto> coinComparisons = expectedSnapshots.stream()
				.map(expected -> {
					CoinSnapshot unsafeCoin = unsafeById.get(expected.id());
					CoinSnapshot safeCoin = safeById.get(expected.id());
					return new CoinComparisonResponseDto(
							expected.id(),
							expected.initialPrice(),
							expected.currentPrice(),
							unsafeCoin.currentPrice(),
							safeCoin.currentPrice(),
							expected.updateCount(),
							unsafeCoin.updateCount(),
							safeCoin.updateCount()
					);
				})
				.toList();

		boolean invariantPassed = coinComparisons.stream()
				.allMatch(c -> c.safe() == c.expected() && c.safeUpdateCount() == c.expectedUpdateCount());

		return new SimulationStatsResponseDto(
				seed, submittedUpdates, workers,
				unsafeProcessedUpdates, safeProcessedUpdates,
				unsafeElapsedMs, safeElapsedMs,
				unsafeThroughput, safeThroughput,
				invariantPassed,
				coinComparisons
		);
	}
}