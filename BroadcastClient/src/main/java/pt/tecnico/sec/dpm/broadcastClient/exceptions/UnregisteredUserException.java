package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class UnregisteredUserException extends Exception {
	@Override
	public String getMessage() {
		return "The user is not registered.";
	}
}
