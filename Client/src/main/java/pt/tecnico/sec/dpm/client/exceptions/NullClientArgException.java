package pt.tecnico.sec.dpm.client.exceptions;

public class NullClientArgException extends Exception {
	@Override
	public String getMessage() {
		return "Some element of the triplet is null.";
	}
}
