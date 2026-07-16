package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskFeederTest {

    @Test
    @DisplayName("Normal akis: tum gorevler + worker sayisi kadar pill kuyruga konur")
    void feedsAllTasksThenPoisonPills() throws Exception {
        TaskQueue queue = new TaskQueue(200);
        List<PriceUpdateTask> tasks = TaskProducer.withDefaultCoins().generate(100, 1L);

        Thread producer = new TaskFeeder(queue, tasks, 2).start();
        producer.join(5_000);

        assertThat(producer.isAlive()).isFalse();
        assertThat(queue.size()).as("100 gorev + 2 pill").isEqualTo(102);
    }

    @Test
    @DisplayName("Interrupt bayragi set'liyken: gorev konmaz ama pill'ler yine de konur")
    void deliversPoisonPillsWhenInterrupted() throws Exception {
        TaskQueue queue = new TaskQueue(50);
        List<PriceUpdateTask> tasks = TaskProducer.withDefaultCoins().generate(100, 1L);
        TaskFeeder feeder = new TaskFeeder(queue, tasks, 3);

        // Bayragi onceden set et -> ilk put() aninda InterruptedException firlatir.
        Thread producer = new Thread(() -> {
            Thread.currentThread().interrupt();
            feeder.run();
        }, "test-feeder");
        producer.start();
        producer.join(5_000);

        assertThat(producer.isAlive()).as("producer asili kalmamali").isFalse();
        assertThat(queue.size()).as("sadece 3 pill konmali").isEqualTo(3);
        for (int i = 0; i < 3; i++) {
            assertThat(TaskQueue.isPoisonPill(queue.take())).isTrue();
        }
    }

    @Test
    @DisplayName("Dolu kuyrukta bloklanmisken interrupt: producer asili kalmaz, kendini kapatir")
    void exitsPromptlyWhenInterruptedWhileBlocked() throws Exception {
        TaskQueue queue = new TaskQueue(10);   // kucuk kuyruk -> producer put()'ta bloklanir
        List<PriceUpdateTask> tasks = TaskProducer.withDefaultCoins().generate(1_000, 1L);

        Thread producer = new TaskFeeder(queue, tasks, 2).start();
        Thread.sleep(150);                     // kuyrugun dolup bloklanmasini bekle
        assertThat(producer.isAlive()).isTrue();

        producer.interrupt();
        producer.join(2_000);

        assertThat(producer.isAlive())
                .as("interrupt sonrasi producer thread'i kapanmali")
                .isFalse();
    }

    @Test
    @DisplayName("Gecersiz argumanlar reddedilir")
    void rejectsInvalidArguments() {
        TaskQueue queue = new TaskQueue(10);
        List<PriceUpdateTask> tasks = TaskProducer.withDefaultCoins().generate(10, 1L);

        assertThatThrownBy(() -> new TaskFeeder(null, tasks, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TaskFeeder(queue, null, 1))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TaskFeeder(queue, tasks, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}