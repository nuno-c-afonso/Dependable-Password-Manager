package pt.tecnico.sec.dpm.broadcastServer.exceptions;

public class DuplicatedNonceException extends Exception {
	@Override
	public String getMessage() {
		return "The given nonce is not new.";
	}
}
