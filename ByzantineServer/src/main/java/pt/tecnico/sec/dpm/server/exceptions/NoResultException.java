package pt.tecnico.sec.dpm.server.exceptions;

public class NoResultException extends Exception {
	@Override
	public String getMessage() {
		return "The query result was empty.";
	}
}
