package com.infina.concurrentcryptopricesimulator.state;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SafeCoinStateTest {

    @Test
    void shouldPreserveAllConcurrentUpdates() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            int workerCount = 8;
            int updatesPerWorker = 2_000;
            SafeCoinState coinState = new SafeCoinState("BTC", 60_000L);
            ExecutorService executor = Executors.newFixedThreadPool(workerCount);
            CountDownLatch startSignal = new CountDownLatch(1);
            List<Future<?>> workers = new ArrayList<>();
            long expectedDelta = 0;

            try {
                for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
                    long delta = workerIndex % 2 == 0 ? 3 : -2;
                    expectedDelta += delta * updatesPerWorker;
                    workers.add(executor.submit(() -> {
                        await(startSignal);
                        for (int updateIndex = 0; updateIndex < updatesPerWorker; updateIndex++) {
                            coinState.applyDelta(delta);
                        }
                    }));
                }

                startSignal.countDown();
                for (Future<?> worker : workers) {
                    worker.get();
                }
            } finally {
                shutdown(executor);
            }

            CoinSnapshot snapshot = coinState.snapshot();
            assertEquals(60_000L + expectedDelta, snapshot.currentPrice());
            assertEquals((long) workerCount * updatesPerWorker, snapshot.updateCount());
        });
    }

    @Test
    void shouldReturnConsistentSnapshotsWhileUpdatesAreRunning() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            int workerCount = 8;
            int updatesPerWorker = 2_000;
            SafeCoinState coinState = new SafeCoinState("ETH", 3_000L);
            ExecutorService executor = Executors.newFixedThreadPool(workerCount + 1);
            CountDownLatch startSignal = new CountDownLatch(1);
            CountDownLatch updatesFinished = new CountDownLatch(workerCount);
            List<Future<?>> workers = new ArrayList<>();

            try {
                for (int workerIndex = 0; workerIndex < workerCount; workerIndex++) {
                    workers.add(executor.submit(() -> {
                        await(startSignal);
                        try {
                            for (int updateIndex = 0; updateIndex < updatesPerWorker; updateIndex++) {
                                coinState.applyDelta(1);
                            }
                        } finally {
                            updatesFinished.countDown();
                        }
                    }));
                }

                Future<Boolean> snapshotReader = executor.submit(() -> {
                    await(startSignal);
                    while (updatesFinished.getCount() > 0) {
                        CoinSnapshot snapshot = coinState.snapshot();
                        if (snapshot.currentPrice()
                                != snapshot.initialPrice() + snapshot.updateCount()) {
                            return false;
                        }
                    }
                    return true;
                });

                startSignal.countDown();
                for (Future<?> worker : workers) {
                    worker.get();
                }

                assertTrue(snapshotReader.get());
            } finally {
                shutdown(executor);
            }

            CoinSnapshot snapshot = coinState.snapshot();
            assertEquals(3_000L + (long) workerCount * updatesPerWorker, snapshot.currentPrice());
            assertEquals((long) workerCount * updatesPerWorker, snapshot.updateCount());
        });
    }

    @Test
    void shouldResetAfterConcurrentUpdates() {
        SafeCoinState coinState = new SafeCoinState("SOL", 150L);
        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            List<Future<?>> updates = new ArrayList<>();
            for (int updateIndex = 0; updateIndex < 1_000; updateIndex++) {
                updates.add(executor.submit(() -> coinState.applyDelta(2)));
            }
            for (Future<?> update : updates) {
                update.get(10, TimeUnit.SECONDS);
            }
        } catch (Exception exception) {
            throw new AssertionError("Concurrent updates did not complete", exception);
        } finally {
            shutdown(executor);
        }

        coinState.reset();

        CoinSnapshot snapshot = coinState.snapshot();
        assertEquals(150L, snapshot.currentPrice());
        assertEquals(0L, snapshot.updateCount());
        assertEquals(0L, snapshot.lastDelta());
        assertNull(snapshot.lastUpdatedBy());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to start", exception);
        }
    }

    private static void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while shutting down executor", exception);
        }
    }
}
