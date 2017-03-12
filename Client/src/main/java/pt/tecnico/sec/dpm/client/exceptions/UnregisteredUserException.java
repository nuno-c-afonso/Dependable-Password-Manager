package pt.tecnico.sec.dpm.client.exceptions;

public class UnregisteredUserException extends Exception {
	@Override
	public String getMessage() {
		return "The user is not registered.";
	}
}
