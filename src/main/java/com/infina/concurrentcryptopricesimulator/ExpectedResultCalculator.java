package com.infina.concurrentcryptopricesimulator;

public class ExpectedResultCalculator {
    public long calculateExpectedResult(int threadCount, int incrementsPerThread) {
        return (long) threadCount * incrementsPerThread;
    }
}