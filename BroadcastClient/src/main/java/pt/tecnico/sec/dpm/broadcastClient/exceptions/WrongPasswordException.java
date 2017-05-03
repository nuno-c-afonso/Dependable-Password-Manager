package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class WrongPasswordException extends Exception {

	@Override
	public String getMessage() {
		return "The Keystore passwords are incorrect.";
	}
	
}
