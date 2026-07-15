package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskQueueTest {

    private static PriceUpdateTask task(long sequence) {
        return new PriceUpdateTask(sequence, "BTC", 10L);
    }

    @Test
    @DisplayName("Gorevler kuyruga konuldugu sirada (FIFO) alinir")
    void preservesFifoOrder() throws Exception {
        TaskQueue queue = new TaskQueue(10);
        queue.put(task(1));
        queue.put(task(2));
        queue.put(task(3));

        assertThat(queue.take().sequence()).isEqualTo(1);
        assertThat(queue.take().sequence()).isEqualTo(2);
        assertThat(queue.take().sequence()).isEqualTo(3);
        assertThat(queue.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Kuyruk doluyken put() bloklanir, yer acilinca devam eder (backpressure)")
    void putBlocksWhenFullAndResumesAfterTake() throws Exception {
        TaskQueue queue = new TaskQueue(2);
        queue.put(task(1));
        queue.put(task(2));
        assertThat(queue.remainingCapacity()).isZero();

        CountDownLatch thirdPutCompleted = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                queue.put(task(3));
                thirdPutCompleted.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "test-producer");
        producer.start();

        assertThat(thirdPutCompleted.await(200, MILLISECONDS))
                .as("dolu kuyrukta put() bloklanmali")
                .isFalse();

        queue.take(); // bir gorev tuket -> yer acilir

        assertThat(thirdPutCompleted.await(2, SECONDS))
                .as("yer acilinca bekleyen put() tamamlanmali")
                .isTrue();
        producer.join(2_000);
        assertThat(queue.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Kuyruk bosken take() bloklanir, gorev gelince doner")
    void takeBlocksWhenEmptyAndResumesAfterPut() throws Exception {
        TaskQueue queue = new TaskQueue(4);
        AtomicReference<PriceUpdateTask> received = new AtomicReference<>();
        CountDownLatch takeCompleted = new CountDownLatch(1);

        Thread consumer = new Thread(() -> {
            try {
                received.set(queue.take());
                takeCompleted.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "test-consumer");
        consumer.start();

        assertThat(takeCompleted.await(200, MILLISECONDS))
                .as("bos kuyrukta take() bloklanmali (thread dump'ta WAITING gorunur)")
                .isFalse();

        PriceUpdateTask expected = task(7);
        queue.put(expected);

        assertThat(takeCompleted.await(2, SECONDS)).isTrue();
        assertThat(received.get()).isEqualTo(expected);
        consumer.join(2_000);
    }

    @Test
    @DisplayName("Worker sayisi kadar poison pill konur ve taninir")
    void putsOnePoisonPillPerWorker() throws Exception {
        TaskQueue queue = new TaskQueue(10);
        queue.put(task(1));
        queue.putPoisonPills(3);

        PriceUpdateTask real = queue.take();
        assertThat(TaskQueue.isPoisonPill(real)).isFalse();

        for (int i = 0; i < 3; i++) {
            assertThat(TaskQueue.isPoisonPill(queue.take()))
                    .as("%d. pill", i + 1)
                    .isTrue();
        }
        assertThat(queue.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("Poison pill'e benzeyen gercek bir gorev pill sanilmaz (kimlik karsilastirmasi)")
    void lookAlikeTaskIsNotMistakenForPoisonPill() {
        PriceUpdateTask lookAlike = new PriceUpdateTask(-1L, "__POISON_PILL__", 0L);

        assertThat(TaskQueue.isPoisonPill(lookAlike)).isFalse();
        assertThat(TaskQueue.isPoisonPill(task(1))).isFalse();
    }

    @Test
    @DisplayName("clear() kuyrugu bosaltir (simulasyonlar arasi sifirlama)")
    void clearEmptiesQueue() throws Exception {
        TaskQueue queue = new TaskQueue(5);
        queue.put(task(1));
        queue.put(task(2));

        queue.clear();

        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.remainingCapacity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Gecersiz kapasite ve gecersiz worker sayisi reddedilir")
    void rejectsInvalidArguments() {
        assertThatThrownBy(() -> new TaskQueue(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TaskQueue(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TaskQueue(4).putPoisonPills(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Kuyruk sinirlidir: kapasite asilamaz")
    void capacityIsEnforced() throws Exception {
        TaskQueue queue = new TaskQueue(3);
        assertThat(queue.capacity()).isEqualTo(3);

        queue.put(task(1));
        queue.put(task(2));
        queue.put(task(3));

        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.remainingCapacity()).isZero();
    }
}