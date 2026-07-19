package com.infina.concurrentcryptopricesimulator.bonus;

import com.infina.concurrentcryptopricesimulator.counter.SafeCounter;
import com.infina.concurrentcryptopricesimulator.engine.TaskProducer;
import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import com.infina.concurrentcryptopricesimulator.repository.DefaultCoinRepositories;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bonus A — Java 21 Virtual Threads karsilastirmasi.
 *
 * <p>Ayni immutable gorev listesi (TaskProducer) hem {@code newFixedThreadPool(n)} hem de
 * {@code newVirtualThreadPerTaskExecutor()} ile islenir ve gecen sureler karsilastirilir.
 * Her kosuda thread-safe (SafeCoinState + SafeCounter) yapilar kullanilir; boylece iki
 * executor da dogru sonuc uretir ve yalnizca zamanlama karsilastirilir.
 *
 * <p>Beklenti: gorevler cok kisa ve CPU-agirlikli (applyDelta + increment) oldugu icin
 * virtual thread'ler burada belirgin bir hiz avantaji SAGLAMAYABILIR; asil kazanimlari
 * bloklayan I/O bekleyen is yuklerindedir.
 */
class VirtualThreadComparisonTest {

    private static final int UPDATES = 50_000;
    private static final int FIXED_POOL_SIZE = 8;
    private static final long SEED = 42L;

    @Test
    @DisplayName("Sabit havuz vs virtual-thread-per-task: ayni is yuku, sure karsilastirmasi")
    void compareFixedPoolVsVirtualThreads() throws Exception {
        List<PriceUpdateTask> tasks = TaskProducer.withDefaultCoins().generate(UPDATES, SEED);

        // 1) Sabit havuz (mevcut mimarideki gibi sinirli sayida yeniden kullanilan thread)
        Result fixed;
        try (ExecutorService fixedPool = Executors.newFixedThreadPool(FIXED_POOL_SIZE)) {
            fixed = runWorkload(fixedPool, tasks);
        }

        // 2) Virtual thread per task (her gorev icin ayri, hafif bir virtual thread)
        Result virtual;
        try (ExecutorService virtualPool = Executors.newVirtualThreadPerTaskExecutor()) {
            virtual = runWorkload(virtualPool, tasks);
        }

        double fixedMs = fixed.elapsedNanos() / 1_000_000.0;
        double virtualMs = virtual.elapsedNanos() / 1_000_000.0;

        System.out.printf("%n===== BONUS A: Virtual Threads Karsilastirmasi (updates=%d) =====%n", UPDATES);
        System.out.printf("Sabit havuz (fixed pool, %d thread) : %.2f ms%n", FIXED_POOL_SIZE, fixedMs);
        System.out.printf("Virtual thread per task            : %.2f ms%n", virtualMs);
        System.out.printf("Oran (virtual / fixed)             : %.2fx%n", virtualMs / fixedMs);
        System.out.println("================================================================");

        // Dogruluk: iki executor da tum gorevleri islemis olmali.
        assertThat(fixed.processed()).isEqualTo(UPDATES);
        assertThat(virtual.processed()).isEqualTo(UPDATES);
    }

    /** Verilen executor ile tum gorevleri isler, gecen sureyi ve islenen gorev sayisini doner. */
    private Result runWorkload(ExecutorService executor, List<PriceUpdateTask> tasks) throws Exception {
        InMemoryCoinRepository<SafeCoinState> repo = DefaultCoinRepositories.createSafe();
        SafeCounter counter = new SafeCounter();

        long start = System.nanoTime();
        List<Future<?>> futures = new ArrayList<>(tasks.size());
        for (PriceUpdateTask task : tasks) {
            futures.add(executor.submit(() -> {
                repo.findById(task.coinId()).ifPresent(coin -> coin.applyDelta(task.delta()));
                counter.increment();
            }));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        long elapsedNanos = System.nanoTime() - start;
        return new Result(elapsedNanos, counter.getValue());
    }

    private record Result(long elapsedNanos, long processed) {
    }
}
