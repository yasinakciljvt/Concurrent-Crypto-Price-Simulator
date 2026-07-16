package com.infina.concurrentcryptopricesimulator;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ExpectedResultCalculatorTest {
    @Test
    public void testCalculateExpectedResult() {
        ExpectedResultCalculator calculator = new ExpectedResultCalculator();
        assertEquals(100000, calculator.calculateExpectedResult(10, 10000));
        assertEquals(0, calculator.calculateExpectedResult(0, 10000));
    }
}