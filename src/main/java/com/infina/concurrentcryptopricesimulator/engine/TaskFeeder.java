package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

// Producer-Consumer'in "uretici" tarafi: hazir gorev listesini kuyruga basar,
// sonuna worker sayisi kadar "dur" isareti (poison pill) koyar.
// Runnable'dir cunku KENDI thread'inde calismak zorundadir (bkz. asagida).
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
            Thread.currentThread().interrupt();
            log.warn("Producer kesintiye ugradi, kuyruga eksik gorev konmus olabilir");
        }
    }

    public Thread start() {
        Thread thread = new Thread(this, THREAD_NAME);
        thread.start();
        return thread;
    }
}