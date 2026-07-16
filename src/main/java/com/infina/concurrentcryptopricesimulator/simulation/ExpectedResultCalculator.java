package com.infina.concurrentcryptopricesimulator.simulation;

public class ExpectedResultCalculator {
    public long calculateExpectedResult(int threadCount, int incrementsPerThread) {
        return (long) threadCount * incrementsPerThread;
    }
}