package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class NullKeystoreElementException extends Exception {
	@Override
	public String getMessage() {
		return "The given Keystore element is null.";
	}
}
