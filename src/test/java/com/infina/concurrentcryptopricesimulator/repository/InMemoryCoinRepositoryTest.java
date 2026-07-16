package com.infina.concurrentcryptopricesimulator.repository;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import com.infina.concurrentcryptopricesimulator.state.UnsafeCoinState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCoinRepositoryTest {

    @Test
    void shouldFindDefaultCoinsInStableOrder() {
        InMemoryCoinRepository<SafeCoinState> repository =
                DefaultCoinRepositories.createSafe();

        List<CoinSnapshot> snapshots = repository.findAllSnapshots();

        assertEquals(List.of("BTC", "ETH", "SOL"), snapshots.stream()
                .map(CoinSnapshot::id)
                .toList());
        assertEquals(List.of(60_000L, 3_000L, 150L), snapshots.stream()
                .map(CoinSnapshot::initialPrice)
                .toList());
        assertTrue(repository.findById("BTC").isPresent());
        assertFalse(repository.findById("DOGE").isPresent());
        assertFalse(repository.findById(null).isPresent());
    }

    @Test
    void shouldRejectInvalidRepositoryContents() {
        assertThrows(
                NullPointerException.class,
                () -> new InMemoryCoinRepository<SafeCoinState>(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new InMemoryCoinRepository<SafeCoinState>(List.of())
        );

        List<SafeCoinState> statesWithNull = new ArrayList<>();
        statesWithNull.add(new SafeCoinState("BTC", 60_000L));
        statesWithNull.add(null);
        assertThrows(
                NullPointerException.class,
                () -> new InMemoryCoinRepository<>(statesWithNull)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new InMemoryCoinRepository<>(List.of(
                        new SafeCoinState("BTC", 60_000L),
                        new SafeCoinState("BTC", 61_000L)
                ))
        );
    }

    @Test
    void shouldReturnUnmodifiableSnapshotList() {
        InMemoryCoinRepository<SafeCoinState> repository =
                DefaultCoinRepositories.createSafe();
        List<CoinSnapshot> snapshots = repository.findAllSnapshots();

        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshots.add(new CoinSnapshot("DOGE", 1, 1, 0, 0, null))
        );
    }

    @Test
    void shouldResetEveryCoin() {
        InMemoryCoinRepository<SafeCoinState> repository =
                DefaultCoinRepositories.createSafe();
        repository.findById("BTC").orElseThrow().applyDelta(100);
        repository.findById("ETH").orElseThrow().applyDelta(-20);
        repository.findById("SOL").orElseThrow().applyDelta(5);

        repository.resetAll();

        for (CoinSnapshot snapshot : repository.findAllSnapshots()) {
            assertEquals(snapshot.initialPrice(), snapshot.currentPrice());
            assertEquals(0L, snapshot.updateCount());
            assertEquals(0L, snapshot.lastDelta());
        }
    }

    @Test
    void shouldCreateIndependentSafeAndUnsafeRepositories() {
        InMemoryCoinRepository<SafeCoinState> safeRepository =
                DefaultCoinRepositories.createSafe();
        InMemoryCoinRepository<UnsafeCoinState> unsafeRepository =
                DefaultCoinRepositories.createUnsafe();

        SafeCoinState safeBitcoin = safeRepository.findById("BTC").orElseThrow();
        UnsafeCoinState unsafeBitcoin = unsafeRepository.findById("BTC").orElseThrow();
        safeBitcoin.applyDelta(500);

        assertNotSame(safeBitcoin, unsafeBitcoin);
        assertEquals(60_500L, safeBitcoin.snapshot().currentPrice());
        assertEquals(60_000L, unsafeBitcoin.snapshot().currentPrice());
    }
}
