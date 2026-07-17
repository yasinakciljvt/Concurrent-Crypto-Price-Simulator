package com.infina.concurrentcryptopricesimulator.api;

import com.infina.concurrentcryptopricesimulator.api.dto.CoinResponseDto;
import com.infina.concurrentcryptopricesimulator.repository.InMemoryCoinRepository;
import com.infina.concurrentcryptopricesimulator.state.SafeCoinState;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SimulationController {
	private final InMemoryCoinRepository<SafeCoinState> safeCoinRepository;

	public SimulationController(InMemoryCoinRepository<SafeCoinState> safeCoinRepository){
		this.safeCoinRepository  = safeCoinRepository;
	}

	@GetMapping("/coins")
	public List<CoinResponseDto> getCoins(){
		return safeCoinRepository.findAllSnapshots().stream()
				.map(CoinResponseDto::from)
				.toList();
	}

}
