package com.infina.concurrentcryptopricesimulator;

public class InvariantChecker {

    // Güvenli sayaç (Safe) her zaman beklenen değere tam eşit olmalıdır.
    public boolean checkSafeCounterInvariant(long actualSafeValue, long expectedValue) {
        return actualSafeValue == expectedValue;
    }

    // Güvensiz sayaç (Unsafe) veri kaybı nedeniyle beklenen değerden küçük veya eşit olabilir.
    public boolean checkUnsafeCounterInvariant(long actualUnsafeValue, long expectedValue) {
        return actualUnsafeValue <= expectedValue;
    }
}