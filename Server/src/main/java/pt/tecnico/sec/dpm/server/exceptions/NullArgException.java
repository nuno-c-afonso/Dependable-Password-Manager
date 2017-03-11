package pt.tecnico.sec.dpm.server.exceptions;

public class NullArgException extends Exception {
	@Override
	public String getMessage() {
		return "One or more of the given arguments is null.";
	}
}
