package com.infina.concurrentcryptopricesimulator.simulation;

import com.infina.concurrentcryptopricesimulator.counter.SafeCounter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationEngineTest {

    @Test
    void delegatesCounterIncrementsToWorkerEngine() throws Exception {
        int threadCount = 4;
        int incrementsPerThread = 250;
        SimulationEngine engine = new SimulationEngine(threadCount, incrementsPerThread);
        SafeCounter counter = new SafeCounter();

        long result = engine.runSimulation(counter);

        assertEquals(threadCount * incrementsPerThread, result);
    }
}
