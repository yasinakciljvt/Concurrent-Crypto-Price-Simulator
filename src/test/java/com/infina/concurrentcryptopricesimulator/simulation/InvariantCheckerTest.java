package com.infina.concurrentcryptopricesimulator.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class InvariantCheckerTest {
    private final InvariantChecker checker = new InvariantChecker();

    @Test
    public void testSafeCounterInvariant() {
        assertTrue(checker.checkSafeCounterInvariant(100, 100));
        assertFalse(checker.checkSafeCounterInvariant(95, 100));
    }

    @Test
    public void testUnsafeCounterInvariant() {
        assertTrue(checker.checkUnsafeCounterInvariant(80, 100));
        assertTrue(checker.checkUnsafeCounterInvariant(100, 100));
        assertFalse(checker.checkUnsafeCounterInvariant(120, 100));
    }
}