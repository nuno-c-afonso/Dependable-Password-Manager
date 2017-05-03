package pt.tecnico.sec.dpm.broadcastServer.exceptions;

public class PublicKeyInvalidSizeException extends Exception {
	@Override
	public String getMessage() {
		return "The given Public Key's size is too large.";
	}
}
