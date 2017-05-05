package pt.tecnico.sec.dpm.client.exceptions;

public class NullKeystoreElementException extends Exception {
	@Override
	public String getMessage() {
		return "The given Keystore element is null.";
	}
}
