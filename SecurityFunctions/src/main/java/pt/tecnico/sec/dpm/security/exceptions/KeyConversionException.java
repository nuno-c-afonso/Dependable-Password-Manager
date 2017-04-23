package pt.tecnico.sec.dpm.security.exceptions;

public class KeyConversionException extends Exception {
	@Override
	public String getMessage() {
		return "The was an error while converting the key format.";
	}
}
