package com.infina.concurrentcryptopricesimulator.api.controller;

import com.infina.concurrentcryptopricesimulator.api.dto.CoinResponseDto;
import com.infina.concurrentcryptopricesimulator.api.dto.ErrorResponseDto;
import com.infina.concurrentcryptopricesimulator.api.dto.SimulationStatsResponseDto;
import com.infina.concurrentcryptopricesimulator.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@Tag(
		name = "Simulation",
		description = "Eşzamanlı kripto fiyat simülasyonu: çalıştırma, güvenli coin durumu ve karşılaştırmalı istatistikler"
)
public class SimulationController {

	private final SimulationService simulationService;

	public SimulationController(SimulationService simulationService) {
		this.simulationService = simulationService;
	}

	@GetMapping("/coins")
	@Operation(
			summary = "Coin durumlarini listeler",
			description = "Son tamamlanan simulasyondaki guvenli (safe) coin durumunu dondurur. "
					+ "Henuz simulasyon calismadiysa coin'lerin baslangic degerleri doner."
	)
	@ApiResponse(responseCode = "200", description = "Guvenli coin durumu")
	public List<CoinResponseDto> getCoins() {
		return simulationService.getCoins();
	}

	@GetMapping("/stats")
	@Operation(
			summary = "Simulasyon istatistiklerini dondurur",
			description = "Son tamamlanan simulasyonun sure, throughput, islenen gorev sayisi ve coin bazli "
					+ "beklenen/guvensiz/guvenli karsilastirmasini dondurur."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Son simulasyonun istatistikleri"),
			@ApiResponse(responseCode = "404", description = "Henuz hic simulasyon calistirilmadi",
					content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
	})
	public SimulationStatsResponseDto getStats() {
		return simulationService.getStats();
	}

	@PostMapping("/simulate")
	@Operation(
			summary = "Simulasyonu calistirir",
			description = "Coin durumlarini sifirlar, verilen seed ile immutable gorev listesini uretir, "
					+ "beklenen sonucu tek thread'de hesaplar, ardindan AYNI listeyle once guvensiz sonra "
					+ "guvenli simulasyonu calistirir. Butun gorevler bitmeden cevap donmez."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Simulasyon tamamlandi"),
			@ApiResponse(responseCode = "400", description = "Gecersiz updates/workers parametresi",
					content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
			@ApiResponse(responseCode = "409", description = "Halihazirda calisan bir simulasyon var",
					content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
	})
	public SimulationStatsResponseDto simulate(
			@Parameter(description = "Uretilecek gorev sayisi", example = "10000")
			@RequestParam @Min(1) @Max(100_000) int updates,
			@Parameter(description = "Worker thread sayisi", example = "4")
			@RequestParam @Min(1) @Max(16) int workers,
			@Parameter(description = "Tekrarlanabilir gorev uretimi icin seed. Verilmezse uretilir ve cevapta doner.",
					example = "42")
			@RequestParam(required = false) Long seed
	) {
		return simulationService.simulate(updates, workers, seed);
	}
}
