package com.infina.concurrentcryptopricesimulator.model;

import java.util.Objects;

/**
 * Tek bir fiyat guncelleme gorevi.
 *
 * @param sequence gorevin uretim sirasi (1'den baslar; kuyruktaki poison pill icin -1)
 * @param coinId   guncellenecek coin (orn. "BTC")
 * @param delta    fiyata eklenecek degisim (negatif olabilir, 0 olmaz)
 */
public record PriceUpdateTask(long sequence, String coinId, long delta) {

    public PriceUpdateTask {
        Objects.requireNonNull(coinId, "coinId null olamaz");
    }

    @Override
    public String toString() {
        return "#%d %s %s%d".formatted(sequence, coinId, delta >= 0 ? "+" : "", delta);
    }
}