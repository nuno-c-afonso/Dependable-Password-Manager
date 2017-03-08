package pt.tecnico.sec.dpm.client.exceptions;

public class WrongPasswordException extends Exception {

	@Override
	public String getMessage() {
		return "The Keystore passwords are incorrect";
	}
	
}
