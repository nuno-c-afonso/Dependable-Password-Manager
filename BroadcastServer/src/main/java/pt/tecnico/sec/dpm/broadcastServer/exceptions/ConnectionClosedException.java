package pt.tecnico.sec.dpm.broadcastServer.exceptions;

public class ConnectionClosedException extends Exception {
	@Override
	public String getMessage() {
		return "The database connection was closed.";
	}
}
