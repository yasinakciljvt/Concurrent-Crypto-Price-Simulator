package com.infina.concurrentcryptopricesimulator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Hata cevabi")
public record ErrorResponseDto(
    @Schema(description = "Hatanin aciklamasi", example = "Another simulation is already running.")
    String message
) {}
