package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskProducerTest {

    private final TaskProducer producer = TaskProducer.withDefaultCoins();

    @Test
    @DisplayName("Ayni seed her zaman birebir ayni gorev listesini uretir (tekrarlanabilirlik)")
    void sameSeedProducesIdenticalTaskList() {
        List<PriceUpdateTask> first = producer.generate(1_000, 42L);
        List<PriceUpdateTask> second = producer.generate(1_000, 42L);

        assertThat(second).containsExactlyElementsOf(first);
    }

    @Test
    @DisplayName("Farkli seed farkli gorev listesi uretir")
    void differentSeedProducesDifferentTaskList() {
        List<PriceUpdateTask> withSeed42 = producer.generate(1_000, 42L);
        List<PriceUpdateTask> withSeed43 = producer.generate(1_000, 43L);

        assertThat(withSeed42).isNotEqualTo(withSeed43);
    }

    @Test
    @DisplayName("Uretim, verilen Map'in iterasyon sirasina bagli degildir")
    void determinismDoesNotDependOnMapIterationOrder() {
        Map<String, Long> bounds = TaskProducer.DEFAULT_MAX_ABS_DELTA;

        List<PriceUpdateTask> fromHashMap = new TaskProducer(new HashMap<>(bounds)).generate(500, 7L);
        List<PriceUpdateTask> fromLinkedMap = new TaskProducer(new LinkedHashMap<>(bounds)).generate(500, 7L);
        List<PriceUpdateTask> fromTreeMap = new TaskProducer(new TreeMap<>(bounds)).generate(500, 7L);

        assertThat(fromLinkedMap).containsExactlyElementsOf(fromHashMap);
        assertThat(fromTreeMap).containsExactlyElementsOf(fromHashMap);
    }

    @Test
    @DisplayName("Istenen sayida gorev uretilir, sequence 1'den baslar ve artarak gider")
    void generatesRequestedCountWithOrderedSequences() {
        List<PriceUpdateTask> tasks = producer.generate(250, 1L);

        assertThat(tasks).hasSize(250);
        for (int i = 0; i < tasks.size(); i++) {
            assertThat(tasks.get(i).sequence()).isEqualTo(i + 1L);
        }
    }

    @Test
    @DisplayName("Donen liste immutable: safe/unsafe kosular arasinda degistirilemez")
    void returnedListIsImmutable() {
        List<PriceUpdateTask> tasks = producer.generate(10, 1L);

        assertThatThrownBy(() -> tasks.add(new PriceUpdateTask(11, "BTC", 5)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> tasks.set(0, new PriceUpdateTask(1, "BTC", 5)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Her gorev tanimli bir coin'e aittir ve delta o coin'in sinirlari icindedir")
    void everyTaskTargetsKnownCoinWithBoundedNonZeroDelta() {
        List<PriceUpdateTask> tasks = producer.generate(5_000, 99L);

        assertThat(tasks).allSatisfy(task -> {
            Long bound = TaskProducer.DEFAULT_MAX_ABS_DELTA.get(task.coinId());
            assertThat(bound).as("bilinmeyen coin: %s", task.coinId()).isNotNull();
            assertThat(task.delta()).isNotZero();
            assertThat(Math.abs(task.delta())).isBetween(1L, bound);
        });
    }

    @Test
    @DisplayName("Yeterli gorevde butun coin'ler kullanilir")
    void allConfiguredCoinsAreUsed() {
        List<PriceUpdateTask> tasks = producer.generate(1_000, 5L);

        assertThat(tasks).extracting(PriceUpdateTask::coinId)
                .containsOnly("BTC", "ETH", "SOL")
                .contains("BTC", "ETH", "SOL");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, 100_001})
    @DisplayName("Sinirlarin disindaki updates degeri reddedilir (API katmani bunu HTTP 400'e cevirir)")
    void rejectsUpdatesOutsideAllowedRange(int updates) {
        assertThatThrownBy(() -> producer.generate(updates, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("updates");
    }

    @Test
    @DisplayName("Gecersiz coin yapilandirmasi reddedilir")
    void rejectsInvalidCoinConfiguration() {
        assertThatThrownBy(() -> new TaskProducer(Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TaskProducer(Map.of("BTC", 0L)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TaskProducer(Map.of("BTC", -5L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Coin id'leri alfabetik sirada acilir")
    void exposesCoinIdsInAlphabeticalOrder() {
        assertThat(producer.coinIds()).containsExactly("BTC", "ETH", "SOL");
    }

    @Test
    @DisplayName("Ozel coin kumesi ve ozel delta sinirlari ile calisir")
    void supportsCustomCoinSet() {
        TaskProducer custom = new TaskProducer(Map.of("DOGE", 2L));
        List<PriceUpdateTask> tasks = custom.generate(100, 3L);

        assertThat(tasks).allSatisfy(task -> {
            assertThat(task.coinId()).isEqualTo("DOGE");
            assertThat(Math.abs(task.delta())).isBetween(1L, 2L);
        });
    }
}