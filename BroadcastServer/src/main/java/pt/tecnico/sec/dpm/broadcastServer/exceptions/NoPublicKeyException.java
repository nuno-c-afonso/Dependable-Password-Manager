package pt.tecnico.sec.dpm.broadcastServer.exceptions;

public class NoPublicKeyException extends Exception {
	@Override
	public String getMessage() {
		return "There is no Public Key that matches the given one.";
	}
}
