package pt.tecnico.sec.dpm.client.exceptions;

public class WrongNonceException extends Exception {
	@Override
	public String getMessage() {
		return "The received nonce is not the expected one.";
	}
}
