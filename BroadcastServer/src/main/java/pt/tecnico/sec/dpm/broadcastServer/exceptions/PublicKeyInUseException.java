package pt.tecnico.sec.dpm.broadcastServer.exceptions;

public class PublicKeyInUseException extends Exception{
	@Override
	public String getMessage() {
		return "The given Public Key is already being used.";
	}
}
