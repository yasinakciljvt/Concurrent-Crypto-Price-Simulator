package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Producer-Consumer'in "uretici" tarafi: hazir gorev listesini kuyruga basar,
 * sonuna worker sayisi kadar poison pill koyar.
 *
 * <p>Runnable'dir cunku KENDI thread'inde calismak zorundadir: kuyruk sinirli oldugu icin
 * dolunca put() bloklanir. Doldurma islemi worker'lari baslatan thread'de yapilirsa,
 * tuketen kimse olmadigi icin sistem kalici olarak kilitlenir.
 *
 * <p><b>Abort sozlesmesi:</b> Bu thread interrupt edilirse poison pill'ler best-effort
 * gonderilir (kuyrukta yer varsa). Kuyruk doluysa gonderilemez; o durumda cagiran taraf
 * worker'lari {@code executor.shutdownNow()} ile kesmek ZORUNDADIR. Poison pill normal
 * bitis mekanizmasidir, abort mekanizmasi degil.
 */
public final class TaskFeeder implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(TaskFeeder.class);

    public static final String THREAD_NAME = "task-producer";

    private final TaskQueue queue;

    // Kuyruga basilacak hazir gorev listesi (TaskProducer uretti).
    private final List<PriceUpdateTask> tasks;

    private final int workerCount;

    // Kurucu: hangi kuyruga, hangi listeyi, kac worker icin akitacagini alir ve dogrular.
    public TaskFeeder(TaskQueue queue, List<PriceUpdateTask> tasks, int workerCount) {
        this.queue = Objects.requireNonNull(queue, "queue null olamaz");
        this.tasks = Objects.requireNonNull(tasks, "tasks null olamaz");
        if (workerCount < 1) {
            throw new IllegalArgumentException("workerCount pozitif olmalidir, gelen: " + workerCount);
        }
        this.workerCount = workerCount;
    }

    @Override
    public void run() {
        try {
            for (PriceUpdateTask task : tasks) {
                queue.put(task);
            }
            queue.putPoisonPills(workerCount);
            log.info("Producer bitti: {} gorev + {} poison pill kuyruga kondu", tasks.size(), workerCount);
        } catch (InterruptedException e) {
            // Kesildik (abort). Worker'lar take()'te asili kalmasin diye pill'leri BEST-EFFORT gonder.
            // put() DEGIL offer(): put() interrupt bayragina bakip aninda tekrar firlatirdi.
            boolean delivered = queue.offerPoisonPills(workerCount);
            Thread.currentThread().interrupt();
            if (delivered) {
                log.warn("Producer kesildi; {} poison pill yine de konuldu, worker'lar temiz cikacak", workerCount);
            } else {
                log.warn("Producer kesildi ve kuyruk dolu oldugu icin pill konamadi. "
                        + "Cagiran taraf executor.shutdownNow() cagirmalidir.");
            }
        }
    }

    public Thread start() {
        Thread thread = new Thread(this, THREAD_NAME);
        thread.start();
        return thread;
    }
}