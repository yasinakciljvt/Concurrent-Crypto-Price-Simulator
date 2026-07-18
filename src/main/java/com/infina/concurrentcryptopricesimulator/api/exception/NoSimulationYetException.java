package com.infina.concurrentcryptopricesimulator.api.exception;

public class NoSimulationYetException extends RuntimeException{
	public NoSimulationYetException(){
		super("No simulation has been run yet.");
	}
}
