package com.infina.concurrentcryptopricesimulator.api.exception;

import com.infina.concurrentcryptopricesimulator.api.dto.ErrorResponseDto;
import com.infina.concurrentcryptopricesimulator.exception.NoSimulationYetException;
import com.infina.concurrentcryptopricesimulator.exception.SimulationAlreadyRunningException;
import com.infina.concurrentcryptopricesimulator.exception.SimulationTimeoutException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationError(ConstraintViolationException exception) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDto(exception.getMessage()));
    }

    @ExceptionHandler(SimulationAlreadyRunningException.class)
    public ResponseEntity<ErrorResponseDto> handleSimulationAlreadyRunning(
            SimulationAlreadyRunningException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDto(exception.getMessage()));
    }

    @ExceptionHandler(NoSimulationYetException.class)
    public ResponseEntity<ErrorResponseDto> handleNoSimulationYet(NoSimulationYetException exception) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDto(exception.getMessage()));
    }

    @ExceptionHandler(SimulationTimeoutException.class)
    public ResponseEntity<ErrorResponseDto> handleSimulationTimeout(
            SimulationTimeoutException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponseDto(exception.getMessage()));
    }
}
