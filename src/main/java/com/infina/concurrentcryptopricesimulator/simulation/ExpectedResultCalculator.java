package com.infina.concurrentcryptopricesimulator.simulation;

import com.infina.concurrentcryptopricesimulator.model.CoinSnapshot;
import com.infina.concurrentcryptopricesimulator.model.PriceUpdateTask;
import com.infina.concurrentcryptopricesimulator.repository.DefaultCoinRepositories;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import com.infina.concurrentcryptopricesimulator.state.UnsafeCoinState;

import java.util.List;

public class ExpectedResultCalculator {
    public long calculateExpectedResult(int threadCount, int incrementsPerThread) {
        return (long) threadCount * incrementsPerThread;
    }

    public long calculateExpectedProcessedTasks(int taskCount) {
        return taskCount;
    }

    public List<CoinSnapshot> calculateExpectedCoinStates(List<PriceUpdateTask> tasks) {
        InMemoryCoinRepository<UnsafeCoinState> expectedRepository = DefaultCoinRepositories.createUnsafe();
        for (PriceUpdateTask task : tasks) {
            expectedRepository.findById(task.coinId()).ifPresent(coin -> coin.applyDelta(task.delta()));
        }
        return expectedRepository.findAllSnapshots();
    }
}