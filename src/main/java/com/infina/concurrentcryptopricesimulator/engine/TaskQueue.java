package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Producer ile worker'lar arasindaki gorev kuyrugu (bekleme salonu).
// Sinirli kapasitelidir ve sonuna "poison pill" (dur isareti) konabilir.
public final class TaskQueue {

    public static final int DEFAULT_CAPACITY = 10_000;

    // "Dur" isareti olan ozel gorev. Tek bir tane vardir ve gercek bir gorevle karismaz.
    // Alanlari (-1, ozel isim) sadece ayirt etmek icin; asil onemli olan bunun TEK nesne olmasi.
    private static final PriceUpdateTask POISON_PILL =
            new PriceUpdateTask(-1L, "__POISON_PILL__", 0L);

    private final BlockingQueue<PriceUpdateTask> queue;

    private final int capacity;

    public TaskQueue() {
        this(DEFAULT_CAPACITY);
    }

    public TaskQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Kuyruk kapasitesi pozitif olmalidir, gelen: " + capacity);
        }
        this.capacity = capacity;
        // Sinirli (bounded) LinkedBlockingQueue: parantez icindeki kapasite onu sinirli yapar.
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    // Kuyruga bir gorev koyar. Kuyruk DOLUYSA yer acilana kadar bekler (backpressure).
    // Bu yuzden yalnizca producer thread'inden cagrilmali.
    public void put(PriceUpdateTask task) throws InterruptedException {
        Objects.requireNonNull(task, "task null olamaz");
        queue.put(task);
    }

    // Kuyruktan bir gorev alir. Kuyruk BOSSA gorev gelene kadar bekler (uyur).
    // Donen sey poison pill olabilir; cagiran isPoisonPill ile kontrol etmeli.
    public PriceUpdateTask take() throws InterruptedException {
        return queue.take();
    }

    // Kuyrugun sonuna worker sayisi kadar "dur" isareti koyar (her worker'a bir tane).
    public void putPoisonPills(int workerCount) throws InterruptedException {
        if (workerCount < 1) {
            throw new IllegalArgumentException("workerCount pozitif olmalidir, gelen: " + workerCount);
        }
        for (int i = 0; i < workerCount; i++) {
            queue.put(POISON_PILL);
        }
    }

    // Best-effort: kuyruga sigdigi kadar pill koyar, BLOKLAMAZ.
    // put()'un aksine interrupt bayragina bakmaz -> kesilmis thread'den de cagrilabilir.
    // Hepsi konabildiyse true, kuyruk dolu oldugu icin konamadiysa false doner.
    public boolean offerPoisonPills(int workerCount) {
        if (workerCount < 1) {
            throw new IllegalArgumentException("workerCount pozitif olmalidir, gelen: " + workerCount);
        }
        for (int i = 0; i < workerCount; i++) {
            if (!queue.offer(POISON_PILL)) {
                return false;
            }
        }
        return true;
    }

    // Gelen gorev "dur" isareti mi? Kimlikle karsilastirir (==), degerle degil.
    // Boylece elle uretilmis benzer bir gorev yanlislikla "dur" sanilmaz.
    public static boolean isPoisonPill(PriceUpdateTask task) {
        return task == POISON_PILL;
    }

    public void clear() {
        queue.clear();
    }

    public int size() {
        return queue.size();
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    public int capacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}