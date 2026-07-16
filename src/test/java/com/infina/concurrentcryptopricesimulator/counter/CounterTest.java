package com.infina.concurrentcryptopricesimulator.counter;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CounterTest {

    @Test
    void testBasicOperations() {
        Counter safeCounter = new SafeCounter();
        Counter unsafeCounter = new UnsafeCounter();

        // Temel artırma testi
        safeCounter.increment();
        unsafeCounter.increment();

        assertEquals(1, safeCounter.getValue());
        assertEquals(1, unsafeCounter.getValue());

        // Reset testi
        safeCounter.reset();
        unsafeCounter.reset();

        assertEquals(0, safeCounter.getValue());
        assertEquals(0, unsafeCounter.getValue());
    }

    @Test
    void testSafeCounterWithConcurrency() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 1000;
        int totalIncrements = threadCount * incrementsPerThread;

        Counter safeCounter = new SafeCounter();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 10 farklı thread aynı anda güvenli sayacı artırıyor
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        safeCounter.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // SafeCounter tek bir güncellemeyi bile kaybetmemelidir
        assertEquals(totalIncrements, safeCounter.getValue(), "SafeCounter veri kaybı yaşamamalıdır!");
    }

    @Test
    void testUnsafeCounterWithConcurrency() throws InterruptedException {
        int threadCount = 10;
        // Yarış durumunu (race condition) tetiklemek için yükü biraz artırıyoruz
        int incrementsPerThread = 10000;
        int totalIncrements = threadCount * incrementsPerThread;

        Counter unsafeCounter = new UnsafeCounter();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 10 farklı thread aynı anda güvensiz sayacı artırıyor
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        unsafeCounter.increment();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        long finalValue = unsafeCounter.getValue();
        System.out.println("--- RACE CONDITION GÖZLEMİ ---");
        System.out.println("Beklenen Toplam Artırım: " + totalIncrements);
        System.out.println("UnsafeCounter Gerçekleşen: " + finalValue);
        System.out.println("Kayıp Veri Miktarı: " + (totalIncrements - finalValue));
        System.out.println("------------------------------");
    }
}