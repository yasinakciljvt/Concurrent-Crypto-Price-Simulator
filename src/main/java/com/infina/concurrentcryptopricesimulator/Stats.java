package com.infina.concurrentcryptopricesimulator;

public class Stats {
    private final long expectedValue;
    private final long actualValue;
    private final long durationMs;

    public Stats(long expectedValue, long actualValue, long durationMs) {
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.durationMs = durationMs;
    }

    public long getExpectedValue() { return expectedValue; }
    public long getActualValue() { return actualValue; }
    public long getDurationMs() { return durationMs; }

    public long getLostUpdates() {
        return Math.max(0, expectedValue - actualValue);
    }

    public double getAccuracyPercentage() {
        if (expectedValue == 0) return 100.0;
        return ((double) actualValue / expectedValue) * 100.0;
    }
}