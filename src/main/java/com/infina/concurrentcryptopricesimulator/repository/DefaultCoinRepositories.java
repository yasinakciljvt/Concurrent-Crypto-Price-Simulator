package com.infina.concurrentcryptopricesimulator.repository;

import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import com.infina.concurrentcryptopricesimulator.state.UnsafeCoinState;

import java.util.List;

public final class DefaultCoinRepositories {

    private static final List<CoinDefinition> DEFAULT_COINS = List.of(
            new CoinDefinition("BTC", 60_000L),
            new CoinDefinition("ETH", 3_000L),
            new CoinDefinition("SOL", 150L)
    );

    private DefaultCoinRepositories() {
    }

    public static InMemoryCoinRepository<SafeCoinState> createSafe() {
        List<SafeCoinState> coinStates = DEFAULT_COINS.stream()
                .map(coin -> new SafeCoinState(coin.id(), coin.initialPrice()))
                .toList();

        return new InMemoryCoinRepository<>(coinStates);
    }

    public static InMemoryCoinRepository<UnsafeCoinState> createUnsafe() {
        List<UnsafeCoinState> coinStates = DEFAULT_COINS.stream()
                .map(coin -> new UnsafeCoinState(coin.id(), coin.initialPrice()))
                .toList();

        return new InMemoryCoinRepository<>(coinStates);
    }

    private record CoinDefinition(String id, long initialPrice) {
    }
}
