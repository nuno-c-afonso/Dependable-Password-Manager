package pt.tecnico.sec.dpm.client.exceptions;

public class NotInitializedException extends Exception {

	
	@Override
	public String getMessage() {
		return "The Distributed password manager client is not initialized";
	}
	
}
