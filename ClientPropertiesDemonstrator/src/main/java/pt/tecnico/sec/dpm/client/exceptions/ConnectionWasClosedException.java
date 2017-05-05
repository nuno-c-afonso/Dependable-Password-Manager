package pt.tecnico.sec.dpm.client.exceptions;

public class ConnectionWasClosedException extends Exception {
	@Override
	public String getMessage() {
		return "The server could not be reached.";
	}
}
