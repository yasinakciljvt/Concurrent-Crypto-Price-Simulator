package com.infina.concurrentcryptopricesimulator.simulation;

import java.time.Duration;

public class SimulationTimeoutException extends RuntimeException {

    public SimulationTimeoutException(int workers, Duration timeout) {
        super("Simülasyon zaman aşımına uğradı (workers=%d, timeout=%ds)"
                .formatted(workers, timeout.toSeconds()));
    }
}
