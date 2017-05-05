package pt.tecnico.sec.dpm.server.exceptions;

public class ConnectionClosedException extends Exception {
	@Override
	public String getMessage() {
		return "The database connection was closed.";
	}
}
