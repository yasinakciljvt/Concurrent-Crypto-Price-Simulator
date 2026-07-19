package com.infina.concurrentcryptopricesimulator.simulation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatsTest {

    @Test
    public void testStatsCalculation() {
        Stats stats = new Stats(100, 80, 150_500_000);

        assertEquals(100, stats.getExpectedValue());
        assertEquals(80, stats.getActualValue());
        assertEquals(150_500_000, stats.getDurationNanos());
        assertEquals(150.5, stats.getDurationMs(), 0.000_001);
        assertEquals(20, stats.getLostUpdates());
        assertEquals(80.0, stats.getAccuracyPercentage());
    }

    @Test
    public void calculatesThroughputFromNanoseconds() {
        Stats stats = new Stats(100, 80, 40_000_000);
        SimulationReport report = new SimulationReport(
                42,
                100,
                4,
                stats,
                stats,
                List.of(),
                List.of(),
                true,
                true
        );

        assertEquals(2_000.0, report.safeThroughputPerSec(), 0.000_001);
        assertEquals(2_000.0, report.unsafeThroughputPerSec(), 0.000_001);
    }
}