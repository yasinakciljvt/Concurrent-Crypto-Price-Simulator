package com.infina.concurrentcryptopricesimulator.repository;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import com.infina.concurrentcryptopricesimulator.state.CoinState;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryCoinRepository<T extends CoinState> {

    private final Map<String, T> coinStatesById;

    public InMemoryCoinRepository(Collection<T> coinStates) {
        Objects.requireNonNull(coinStates, "Coin durumları null olamaz.");

        if (coinStates.isEmpty()) {
            throw new IllegalArgumentException("Coin repository boş olamaz.");
        }

        Map<String, T> statesById = new LinkedHashMap<>();
        for (T coinState : coinStates) {
            T nonNullCoinState = Objects.requireNonNull(
                    coinState,
                    "Coin durumu null olamaz."
            );
            T existingState = statesById.putIfAbsent(nonNullCoinState.id(), nonNullCoinState);
            if (existingState != null) {
                throw new IllegalArgumentException(
                        "Tekrarlanan coin kimliği: " + nonNullCoinState.id()
                );
            }
        }

        coinStatesById = Collections.unmodifiableMap(statesById);
    }

    public Optional<T> findById(String coinId) {
        return Optional.ofNullable(coinStatesById.get(coinId));
    }

    public List<CoinSnapshot> findAllSnapshots() {
        return coinStatesById.values()
                .stream()
                .map(CoinState::snapshot)
                .toList();
    }

    public void resetAll() {
        coinStatesById.values().forEach(CoinState::reset);
    }
}
