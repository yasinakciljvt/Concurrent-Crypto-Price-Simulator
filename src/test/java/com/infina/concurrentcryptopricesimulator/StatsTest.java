package com.infina.concurrentcryptopricesimulator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StatsTest {
    @Test
    public void testStatsCalculation() {
        Stats stats = new Stats(100, 80, 150);
        assertEquals(100, stats.getExpectedValue());
        assertEquals(80, stats.getActualValue());
        assertEquals(150, stats.getDurationMs());
        assertEquals(20, stats.getLostUpdates());
        assertEquals(80.0, stats.getAccuracyPercentage());
    }
}