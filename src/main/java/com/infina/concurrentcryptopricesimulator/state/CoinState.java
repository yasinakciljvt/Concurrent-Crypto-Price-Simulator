package com.infina.concurrentcryptopricesimulator.state;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;

public interface CoinState {

    String id();

    long initialPrice();

    void applyDelta(long delta);

    void reset();

    CoinSnapshot snapshot();
}
