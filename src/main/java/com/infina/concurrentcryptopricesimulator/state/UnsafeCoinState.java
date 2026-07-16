package com.infina.concurrentcryptopricesimulator.state;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;

public final class UnsafeCoinState implements CoinState {

    private final String id;
    private final long initialPrice;
    private long currentPrice;
    private long updateCount;
    private long lastDelta;
    private String lastUpdatedBy;

    public UnsafeCoinState(String id, long initialPrice) {
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
        currentPrice += delta;
        updateCount++;
        lastDelta = delta;
        lastUpdatedBy = Thread.currentThread().getName();
    }

    @Override
    public void reset() {
        currentPrice = initialPrice;
        updateCount = 0;
        lastDelta = 0;
        lastUpdatedBy = null;
    }

    @Override
    public CoinSnapshot snapshot() {
        return new CoinSnapshot(
                id,
                initialPrice,
                currentPrice,
                updateCount,
                lastDelta,
                lastUpdatedBy
        );
    }

    private static String requireValidId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Coin id must not be blank");
        }

        return id;
    }
}
