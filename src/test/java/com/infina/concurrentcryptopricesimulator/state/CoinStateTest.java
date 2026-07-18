package com.infina.concurrentcryptopricesimulator.state;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CoinStateTest {

    private static final List<BiFunction<String, Long, CoinState>> COIN_STATE_FACTORIES =
            List.of(SafeCoinState::new, UnsafeCoinState::new);

    @Test
    void shouldCreateInitialSnapshot() {
        for (BiFunction<String, Long, CoinState> factory : COIN_STATE_FACTORIES) {
            CoinState coinState = factory.apply("BTC", 60_000L);

            CoinSnapshot snapshot = coinState.snapshot();

            assertEquals("BTC", snapshot.id());
            assertEquals(60_000L, snapshot.initialPrice());
            assertEquals(60_000L, snapshot.currentPrice());
            assertEquals(0L, snapshot.updateCount());
            assertEquals(0L, snapshot.lastDelta());
            assertNull(snapshot.lastUpdatedBy());
        }
    }

    @Test
    void shouldRejectNullOrBlankCoinId() {
        for (BiFunction<String, Long, CoinState> factory : COIN_STATE_FACTORIES) {
            assertThrows(IllegalArgumentException.class, () -> factory.apply(null, 100L));
            assertThrows(IllegalArgumentException.class, () -> factory.apply("", 100L));
            assertThrows(IllegalArgumentException.class, () -> factory.apply("   ", 100L));
        }
    }

    @Test
    void shouldApplySequentialDeltasAndTrackLastUpdate() {
        for (BiFunction<String, Long, CoinState> factory : COIN_STATE_FACTORIES) {
            CoinState coinState = factory.apply("ETH", 3_000L);

            coinState.applyDelta(120);
            coinState.applyDelta(-25);
            coinState.applyDelta(0);

            CoinSnapshot snapshot = coinState.snapshot();
            assertEquals(3_095L, snapshot.currentPrice());
            assertEquals(3L, snapshot.updateCount());
            assertEquals(0L, snapshot.lastDelta());
            assertEquals(Thread.currentThread().getName(), snapshot.lastUpdatedBy());
        }
    }

    @Test
    void shouldResetAndRemainReusable() {
        for (BiFunction<String, Long, CoinState> factory : COIN_STATE_FACTORIES) {
            CoinState coinState = factory.apply("SOL", 150L);
            coinState.applyDelta(30);

            coinState.reset();

            CoinSnapshot resetSnapshot = coinState.snapshot();
            assertEquals(150L, resetSnapshot.currentPrice());
            assertEquals(0L, resetSnapshot.updateCount());
            assertEquals(0L, resetSnapshot.lastDelta());
            assertNull(resetSnapshot.lastUpdatedBy());

            coinState.applyDelta(-10);

            CoinSnapshot reusedSnapshot = coinState.snapshot();
            assertEquals(140L, reusedSnapshot.currentPrice());
            assertEquals(1L, reusedSnapshot.updateCount());
            assertEquals(-10L, reusedSnapshot.lastDelta());
        }
    }
}
