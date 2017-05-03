package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class NullClientArgException extends Exception {
	@Override
	public String getMessage() {
		return "Some element of the triplet is null.";
	}
}
