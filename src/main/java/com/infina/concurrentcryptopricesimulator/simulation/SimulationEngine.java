package com.infina.concurrentcryptopricesimulator.simulation;

import com.infina.concurrentcryptopricesimulator.counter.Counter;
import com.infina.concurrentcryptopricesimulator.counter.SafeCounter;
import com.infina.concurrentcryptopricesimulator.counter.UnsafeCounter;
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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Service
public class SimulationEngine {

    private static final Duration WORKER_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration FEEDER_JOIN_TIMEOUT = Duration.ofSeconds(5);

    private final TaskProducer taskProducer;
    private final InMemoryCoinRepository<SafeCoinState> safeCoinRepository;
    private final ExpectedResultCalculator expectedResultCalculator;
    private final InvariantChecker invariantChecker;

    public SimulationEngine(TaskProducer taskProducer,
                            InMemoryCoinRepository<SafeCoinState> safeCoinRepository) {
        this.taskProducer = taskProducer;
        this.safeCoinRepository = safeCoinRepository;
        this.expectedResultCalculator = new ExpectedResultCalculator();
        this.invariantChecker = new InvariantChecker();
    }

    public SimulationReport runFullSimulation(int updates, int workers, Long seed) throws InterruptedException {
        safeCoinRepository.resetAll();

        long usedSeed = seed != null ? seed : TaskProducer.randomSeed();
        List<PriceUpdateTask> tasks = taskProducer.generate(updates, usedSeed);

        List<CoinSnapshot> expectedSnapshots = expectedResultCalculator.calculateExpectedCoinStates(tasks);
        long expectedProcessedTasks = expectedResultCalculator.calculateExpectedProcessedTasks(tasks.size());

        Counter unsafeCounter = new UnsafeCounter();
        InMemoryCoinRepository<UnsafeCoinState> unsafeRepository = DefaultCoinRepositories.createUnsafe();
        long unsafeElapsedMs = runPass(tasks, workers, task -> {
            unsafeRepository.findById(task.coinId()).ifPresent(coin -> coin.applyDelta(task.delta()));
            unsafeCounter.increment();
        });

        Counter safeCounter = new SafeCounter();
        long safeElapsedMs = runPass(tasks, workers, task -> {
            safeCoinRepository.findById(task.coinId()).ifPresent(coin -> coin.applyDelta(task.delta()));
            safeCounter.increment();
        });

        Stats safeStats = new Stats(expectedProcessedTasks, safeCounter.getValue(), safeElapsedMs);
        Stats unsafeStats = new Stats(expectedProcessedTasks, unsafeCounter.getValue(), unsafeElapsedMs);

        List<CoinSnapshot> safeSnapshots = safeCoinRepository.findAllSnapshots();
        List<CoinComparison> coinComparisons = buildCoinComparisons(
                expectedSnapshots, unsafeRepository.findAllSnapshots(), safeSnapshots);

        boolean safeInvariantValid =
                invariantChecker.checkSafeCounterInvariant(safeStats.getActualValue(), safeStats.getExpectedValue())
                        && invariantChecker.checkSafeCoinInvariants(coinComparisons);
        boolean unsafeInvariantValid =
                invariantChecker.checkUnsafeCounterInvariant(unsafeStats.getActualValue(), unsafeStats.getExpectedValue());

        return new SimulationReport(
                usedSeed,
                tasks.size(),
                workers,
                safeStats,
                unsafeStats,
                coinComparisons,
                safeSnapshots,
                safeInvariantValid,
                unsafeInvariantValid
        );
    }

    public Stats runSimulation(Counter counter, int workers, List<PriceUpdateTask> tasks) throws InterruptedException {
        long expectedValue = expectedResultCalculator.calculateExpectedProcessedTasks(tasks.size());
        long durationMs = runPass(tasks, workers, task -> counter.increment());
        return new Stats(expectedValue, counter.getValue(), durationMs);
    }

    private long runPass(List<PriceUpdateTask> tasks, int workers, PriceTaskProcessor processor)
            throws InterruptedException {
        TaskQueue queue = new TaskQueue();
        CountDownLatch completionLatch = new CountDownLatch(tasks.size());
        WorkerEngine workerEngine = new WorkerEngine(workers);

        long startTimer = System.currentTimeMillis();
        workerEngine.startWorkers(queue, completionLatch, processor);
        Thread feederThread = new TaskFeeder(queue, tasks, workers).start();

        boolean completed = workerEngine.awaitCompletion(completionLatch, WORKER_TIMEOUT);
        long elapsedMs = System.currentTimeMillis() - startTimer;

        if (!completed) {
            feederThread.interrupt();
        }
        feederThread.join(FEEDER_JOIN_TIMEOUT.toMillis());
        workerEngine.shutdownGracefully();

        if (!completed) {
            throw new SimulationTimeoutException(workers, WORKER_TIMEOUT);
        }
        return elapsedMs;
    }

    private List<CoinComparison> buildCoinComparisons(List<CoinSnapshot> expectedSnapshots,
                                                      List<CoinSnapshot> unsafeSnapshots,
                                                      List<CoinSnapshot> safeSnapshots) {
        Map<String, CoinSnapshot> unsafeById = unsafeSnapshots.stream()
                .collect(Collectors.toMap(CoinSnapshot::id, snapshot -> snapshot));
        Map<String, CoinSnapshot> safeById = safeSnapshots.stream()
                .collect(Collectors.toMap(CoinSnapshot::id, snapshot -> snapshot));

        return expectedSnapshots.stream()
                .map(expected -> {
                    CoinSnapshot unsafeCoin = unsafeById.get(expected.id());
                    CoinSnapshot safeCoin = safeById.get(expected.id());
                    return new CoinComparison(
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
    }
}
