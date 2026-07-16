package com.infina.concurrentcryptopricesimulator.counter;

import java.util.concurrent.atomic.AtomicLong;

public class SafeCounter implements Counter {
    private final AtomicLong count = new AtomicLong(0);

    @Override
    public void increment() {

        count.incrementAndGet();
    }

    @Override
    public long getValue() {
        return count.get();
    }

    @Override
    public void reset() {
        count.set(0);
    }
}