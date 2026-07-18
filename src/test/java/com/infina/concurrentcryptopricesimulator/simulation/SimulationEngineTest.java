package com.infina.concurrentcryptopricesimulator.simulation;

import com.infina.concurrentcryptopricesimulator.counter.SafeCounter;
import com.infina.concurrentcryptopricesimulator.engine.TaskProducer;
import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import com.infina.concurrentcryptopricesimulator.repository.DefaultCoinRepositories;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    private SimulationEngine newEngine() {
        return new SimulationEngine(TaskProducer.withDefaultCoins(), DefaultCoinRepositories.createSafe());
    }

    @Test
    void safeRunProcessesEveryTaskAndSatisfiesInvariant() throws Exception {
        SimulationReport report = newEngine().runFullSimulation(5_000, 4, 42L);

        assertEquals(42L, report.seed());
        assertEquals(5_000, report.submittedUpdates());
        assertEquals(4, report.workers());
        assertEquals(5_000L, report.safeStats().getExpectedValue());
        assertEquals(5_000L, report.safeStats().getActualValue());
        assertEquals(0L, report.safeStats().getLostUpdates());
        assertTrue(report.safeInvariantValid());
    }

    @Test
    void safeCoinPricesMatchSingleThreadedExpectation() throws Exception {
        SimulationReport report = newEngine().runFullSimulation(5_000, 8, 7L);

        for (CoinComparison comparison : report.coinComparisons()) {
            assertEquals(comparison.expectedPrice(), comparison.safePrice(), comparison.id() + " fiyati");
            assertEquals(comparison.expectedUpdateCount(), comparison.safeUpdateCount(), comparison.id() + " guncelleme sayisi");
        }
    }

    @Test
    void unsafeRunNeverExceedsExpectedValue() throws Exception {
        SimulationReport report = newEngine().runFullSimulation(5_000, 8, 13L);

        assertTrue(report.unsafeInvariantValid());
        assertTrue(report.unsafeStats().getActualValue() <= report.unsafeStats().getExpectedValue());
    }

    @Test
    void sameSeedProducesSameExpectedPrices() throws Exception {
        SimulationReport first = newEngine().runFullSimulation(2_000, 4, 99L);
        SimulationReport second = newEngine().runFullSimulation(2_000, 4, 99L);

        assertEquals(
                first.coinComparisons().stream().map(CoinComparison::expectedPrice).toList(),
                second.coinComparisons().stream().map(CoinComparison::expectedPrice).toList()
        );
    }

    @Test
    void seedIsGeneratedAndReportedWhenNotSupplied() throws Exception {
        SimulationReport report = newEngine().runFullSimulation(500, 2, null);

        assertEquals(500, report.submittedUpdates());
        assertTrue(report.safeInvariantValid());
    }

    @Test
    void safeCounterKeepsEveryIncrementWhenRunOverTaskList() throws Exception {
        List<PriceUpdateTask> tasks = TaskProducer.withDefaultCoins().generate(4_000, 21L);
        SafeCounter counter = new SafeCounter();

        Stats stats = newEngine().runSimulation(counter, 4, tasks);

        assertEquals(4_000L, stats.getExpectedValue());
        assertEquals(4_000L, stats.getActualValue());
    }
}
