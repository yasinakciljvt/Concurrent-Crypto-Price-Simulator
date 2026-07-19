package com.infina.concurrentcryptopricesimulator.exception;

public class NoSimulationYetException extends RuntimeException {

    public NoSimulationYetException() {
        super("Henüz bir simülasyon çalıştırılmadı.");
    }
}
