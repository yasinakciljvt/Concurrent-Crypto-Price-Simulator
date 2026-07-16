package com.infina.concurrentcryptopricesimulator.state;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import com.infina.concurrentcryptopricesimulator.repository.DefaultCoinRepositories;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinInvariantTest {

    @Test
    void shouldSatisfyPriceAndUpdateCountInvariantsForEveryCoin() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            InMemoryCoinRepository<SafeCoinState> repository =
                    DefaultCoinRepositories.createSafe();
            List<CoinUpdate> updates = createDeterministicUpdates(6_000);
            Map<String, Long> expectedDeltaByCoin = new HashMap<>();
            Map<String, Long> expectedCountByCoin = new HashMap<>();

            for (CoinUpdate update : updates) {
                expectedDeltaByCoin.merge(update.coinId(), update.delta(), Long::sum);
                expectedCountByCoin.merge(update.coinId(), 1L, Long::sum);
            }

            applyConcurrently(repository, updates);

            Map<String, Long> initialPriceByCoin = Map.of(
                    "BTC", 60_000L,
                    "ETH", 3_000L,
                    "SOL", 150L
            );
            for (CoinSnapshot snapshot : repository.findAllSnapshots()) {
                long expectedPrice = initialPriceByCoin.get(snapshot.id())
                        + expectedDeltaByCoin.getOrDefault(snapshot.id(), 0L);
                long expectedUpdateCount = expectedCountByCoin.getOrDefault(snapshot.id(), 0L);

                assertEquals(expectedPrice, snapshot.currentPrice());
                assertEquals(expectedUpdateCount, snapshot.updateCount());
            }
        });
    }

    private static List<CoinUpdate> createDeterministicUpdates(int updateCount) {
        String[] coinIds = {"BTC", "ETH", "SOL"};
        long[] deltas = {120, -25, 3, -50, 80, -2, 0};
        List<CoinUpdate> updates = new ArrayList<>(updateCount);

        for (int sequence = 0; sequence < updateCount; sequence++) {
            updates.add(new CoinUpdate(
                    coinIds[sequence % coinIds.length],
                    deltas[sequence % deltas.length]
            ));
        }

        return List.copyOf(updates);
    }

    private static void applyConcurrently(
            InMemoryCoinRepository<SafeCoinState> repository,
            List<CoinUpdate> updates
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch startSignal = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(updates.size());

        try {
            for (CoinUpdate update : updates) {
                futures.add(executor.submit(() -> {
                    await(startSignal);
                    repository.findById(update.coinId())
                            .orElseThrow()
                            .applyDelta(update.delta());
                }));
            }

            startSignal.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to start", exception);
        }
    }

    private record CoinUpdate(String coinId, long delta) {
    }
}
