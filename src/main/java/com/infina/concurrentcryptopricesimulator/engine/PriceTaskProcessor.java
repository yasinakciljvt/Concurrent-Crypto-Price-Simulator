package com.infina.concurrentcryptopricesimulator.engine;

import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;

@FunctionalInterface
public interface PriceTaskProcessor {

    void process(PriceUpdateTask task);
}
