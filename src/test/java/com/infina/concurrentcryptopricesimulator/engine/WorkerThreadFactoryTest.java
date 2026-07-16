package com.infina.concurrentcryptopricesimulator.engine;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerThreadFactoryTest {

    @Test
    void namesThreadsWorker1Worker2Worker3() {
        WorkerThreadFactory factory = new WorkerThreadFactory();
        Set<String> names = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            Thread thread = factory.newThread(() -> {
            });
            names.add(thread.getName());
        }

        assertEquals(3, names.size());
        assertTrue(names.contains("worker-1"));
        assertTrue(names.contains("worker-2"));
        assertTrue(names.contains("worker-3"));
    }
}
