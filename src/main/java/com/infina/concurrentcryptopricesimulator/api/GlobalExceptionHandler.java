package com.infina.concurrentcryptopricesimulator.api;

import com.infina.concurrentcryptopricesimulator.api.dto.ErrorResponseDto;
import com.infina.concurrentcryptopricesimulator.simulation.SimulationTimeoutException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponseDto> handleValidationError(ConstraintViolationException e){
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponseDto(e.getMessage()));
	}

	@ExceptionHandler(SimulationAlreadyRunningException.class)
	public ResponseEntity<ErrorResponseDto> handleSimulationAlreadyRunning(SimulationAlreadyRunningException e){
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponseDto(e.getMessage()));
	}

	@ExceptionHandler(NoSimulationYetException.class)
	public ResponseEntity<ErrorResponseDto> handleNoSimulationYet(NoSimulationYetException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponseDto(e.getMessage()));
	}

	@ExceptionHandler(SimulationTimeoutException.class)
	public ResponseEntity<ErrorResponseDto> handleSimulationTimeout(SimulationTimeoutException e) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ErrorResponseDto(e.getMessage()));
	}

}
