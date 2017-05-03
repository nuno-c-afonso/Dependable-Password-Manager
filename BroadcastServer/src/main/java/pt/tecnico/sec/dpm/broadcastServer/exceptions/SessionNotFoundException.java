package pt.tecnico.sec.dpm.broadcastServer.exceptions;

public class SessionNotFoundException extends Exception {
	@Override
	public String getMessage() {
		return "The given session was not found.";
	}
}
