package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class NotInitializedException extends Exception {

	
	@Override
	public String getMessage() {
		return "The Distributed Password Manager client is not initialized.";
	}
	
}
