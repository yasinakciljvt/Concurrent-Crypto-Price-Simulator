package com.infina.concurrentcryptopricesimulator.counter;

public interface Counter {
    void increment();
    long getValue();
    void reset();
}
