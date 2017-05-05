package pt.tecnico.sec.dpm.server.exceptions;

public class NoPasswordException extends Exception {
	@Override
	public String getMessage() {
		return "There is no password for the given domain and username.";
	}
}
