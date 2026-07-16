package com.infina.concurrentcryptopricesimulator.counter;

public class UnsafeCounter implements Counter{
    private long count = 0;

    @Override
    public void increment() {
        // Oku -> Artır -> Yaz adımları arasında diğer thread'ler araya girebilir[cite: 1]
        count++;
    }

    @Override
    public long getValue() {
        return count;
    }

    @Override
    public void reset() {
        count = 0;
    }
}
