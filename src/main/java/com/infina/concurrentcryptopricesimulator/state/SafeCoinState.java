package com.infina.concurrentcryptopricesimulator.state;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;

import java.util.concurrent.locks.ReentrantLock;

public final class SafeCoinState implements CoinState {

    private final String id;
    private final long initialPrice;
    private final ReentrantLock lock = new ReentrantLock();
    private long currentPrice;
    private long updateCount;
    private long lastDelta;
    private String lastUpdatedBy;

    public SafeCoinState(String id, long initialPrice) {
        this.id = requireValidId(id);
        this.initialPrice = initialPrice;
        this.currentPrice = initialPrice;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public long initialPrice() {
        return initialPrice;
    }

    @Override
    public void applyDelta(long delta) {
        lock.lock();
        try {
            currentPrice += delta;
            updateCount++;
            lastDelta = delta;
            lastUpdatedBy = Thread.currentThread().getName();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reset() {
        lock.lock(); // safe için lock atıyoruz
        try {
            currentPrice = initialPrice;
            updateCount = 0;
            lastDelta = 0;
            lastUpdatedBy = null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CoinSnapshot snapshot() {
        lock.lock();
        try {
            return new CoinSnapshot(
                    id,
                    initialPrice,
                    currentPrice,
                    updateCount,
                    lastDelta,
                    lastUpdatedBy
            );
        } finally {
            lock.unlock();
        }
    }

    private static String requireValidId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Coin id must not be blank");
        }

        return id;
    }
}
