package pt.tecnico.sec.dpm.server.exceptions;

public class DuplicatedNonceException extends Exception {
	@Override
	public String getMessage() {
		return "The given nonce is not new.";
	}
}
