package pt.tecnico.sec.dpm.client.exceptions;

public class AlreadyInitializedException extends Exception {

	
	@Override
	public String getMessage() {
		return "The Distributed Password Manager client is already initialized.";
	}
	
}
