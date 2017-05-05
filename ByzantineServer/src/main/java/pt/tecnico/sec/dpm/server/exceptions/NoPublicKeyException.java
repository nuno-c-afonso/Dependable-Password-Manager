package pt.tecnico.sec.dpm.server.exceptions;

public class NoPublicKeyException extends Exception {
	@Override
	public String getMessage() {
		return "There is no Public Key that matches the given one.";
	}
}
