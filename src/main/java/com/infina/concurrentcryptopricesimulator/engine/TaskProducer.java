package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;



// Bu sinif bir "gorev fabrikasidir": seed verirsin, dondurulmus bir gorev listesi uretir.
// Kuyruk, worker, coin gibi seylere karismaz; sadece PriceUpdateTask listesi olusturur.
public final class  TaskProducer {

    public static final int MIN_UPDATES = 1;
    public static final int MAX_UPDATES = 100_000;

    public static final Map<String, Long> DEFAULT_MAX_ABS_DELTA =
            Map.of("BTC", 200L, "ETH", 30L, "SOL", 5L);

    private final List<String> coinIds;
    private final Map<String, Long> maxAbsDeltaByCoin;


    public TaskProducer(Map<String, Long> maxAbsDeltaByCoin) {
        Objects.requireNonNull(maxAbsDeltaByCoin, "maxAbsDeltaByCoin null olamaz");
        if (maxAbsDeltaByCoin.isEmpty()) {
            throw new IllegalArgumentException("En az bir coin tanimlanmalidir");
        }
        maxAbsDeltaByCoin.forEach((coinId, bound) -> {
            Objects.requireNonNull(coinId, "coinId null olamaz");
            if (bound == null || bound < 1) {
                throw new IllegalArgumentException(
                        "Coin '%s' icin delta siniri pozitif olmalidir, gelen: %s".formatted(coinId, bound));
            }
        });

        // Alfabetik sira -> caller hangi Map tipini verirse versin uretim deterministik kalir.
        this.coinIds = maxAbsDeltaByCoin.keySet().stream().sorted().toList();
        this.maxAbsDeltaByCoin = new LinkedHashMap<>(maxAbsDeltaByCoin);
    }

    public static TaskProducer withDefaultCoins() {
        return new TaskProducer(DEFAULT_MAX_ABS_DELTA);
    }


    public List<PriceUpdateTask> generate(int updates, long seed) {
        if (updates < MIN_UPDATES || updates > MAX_UPDATES) {
            throw new IllegalArgumentException(
                    "updates %d ile %d arasinda olmalidir, gelen: %d".formatted(MIN_UPDATES, MAX_UPDATES, updates));
        }

        Random random = new Random(seed);
        List<PriceUpdateTask> tasks = new ArrayList<>(updates);

        for (int i = 0; i < updates; i++) {
            String coinId = coinIds.get(random.nextInt(coinIds.size()));
            long bound = maxAbsDeltaByCoin.get(coinId);

            // [1, bound] araligindan buyukluk sec, sonra isareti at: delta hicbir zaman 0 olmaz.
            // Delta 0 olsaydi "gorev islendi ama fiyat degismedi" durumu race condition'i maskelerdi.
            long magnitude = 1 + random.nextLong(bound);
            long delta = random.nextBoolean() ? magnitude : -magnitude;

            tasks.add(new PriceUpdateTask(i + 1L, coinId, delta));
        }

        return List.copyOf(tasks);
    }

    /** seed verilmediginde kullanilir; uretilen seed /stats cevabinda raporlanmalidir. */
    public static long randomSeed() {
        return new Random().nextLong();
    }

    /** Ureticinin bildigi coin id'leri, alfabetik ve degistirilemez. */
    public List<String> coinIds() {
        return coinIds;
    }
}