package com.infina.concurrentcryptopricesimulator.api.dto;

import jakarta.validation.constraints.NotNull;

public record CoinDto(
        String id,
        @NotNull long initialPrice,
        long currentPrice,
        long updateCount,
        long lastDelta,
        String lastUpdatedBy
) {}
