package com.infina.concurrentcryptopricesimulator.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulation")
public class SimulationController {

	@ResponseStatus(HttpStatus.OK)
	@GetMapping
	public String test() {
		return "Test";
	}
}
