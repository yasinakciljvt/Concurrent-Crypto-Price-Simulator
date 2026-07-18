package com.infina.concurrentcryptopricesimulator.simulation;

import java.util.List;

public class InvariantChecker {

    // Güvenli sayaç (Safe) her zaman beklenen değere tam eşit olmalıdır.
    public boolean checkSafeCounterInvariant(long actualSafeValue, long expectedValue) {
        return actualSafeValue == expectedValue;
    }

    // Güvensiz sayaç (Unsafe) veri kaybı nedeniyle beklenen değerden küçük veya eşit olabilir.
    public boolean checkUnsafeCounterInvariant(long actualUnsafeValue, long expectedValue) {
        return actualUnsafeValue <= expectedValue;
    }

    public boolean checkSafeCoinInvariant(CoinComparison comparison) {
        return comparison.safePrice() == comparison.expectedPrice()
                && comparison.safeUpdateCount() == comparison.expectedUpdateCount();
    }

    public boolean checkSafeCoinInvariants(List<CoinComparison> comparisons) {
        return comparisons.stream().allMatch(this::checkSafeCoinInvariant);
    }
}