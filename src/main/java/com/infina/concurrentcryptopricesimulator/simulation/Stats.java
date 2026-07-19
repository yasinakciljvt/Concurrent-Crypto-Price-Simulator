package com.infina.concurrentcryptopricesimulator.simulation;

public class Stats {
    private final long expectedValue;
    private final long actualValue;
    private final long durationNanos;

    public Stats(long expectedValue, long actualValue, long durationNanos) {
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.durationNanos = durationNanos;
    }

    public long getExpectedValue() { return expectedValue; }
    public long getActualValue() { return actualValue; }
    public long getDurationNanos() { return durationNanos; }
    public double getDurationMs() { return durationNanos / 1_000_000.0; }

    public long getLostUpdates() {
        return Math.max(0, expectedValue - actualValue);
    }

    public double getAccuracyPercentage() {
        if (expectedValue == 0) return 100.0;
        return ((double) actualValue / expectedValue) * 100.0;
    }
}