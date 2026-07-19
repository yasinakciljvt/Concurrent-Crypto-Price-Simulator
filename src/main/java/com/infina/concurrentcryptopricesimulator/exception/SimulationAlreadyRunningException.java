package com.infina.concurrentcryptopricesimulator.exception;

public class SimulationAlreadyRunningException extends RuntimeException {

    public SimulationAlreadyRunningException() {
        super("Başka bir simülasyon zaten çalışıyor.");
    }
}
