package com.infina.concurrentcryptopricesimulator.api;

public class SimulationAlreadyRunningException extends RuntimeException{
	public SimulationAlreadyRunningException(){
		super("Another simulation is already running.");
	}
}
