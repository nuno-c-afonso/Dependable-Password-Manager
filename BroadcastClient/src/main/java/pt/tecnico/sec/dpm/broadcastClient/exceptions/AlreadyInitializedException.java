package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class AlreadyInitializedException extends Exception {

	
	@Override
	public String getMessage() {
		return "The Distributed Password Manager client is already initialized.";
	}
	
}
