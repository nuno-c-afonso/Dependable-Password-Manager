package pt.tecnico.sec.dpm.security.exceptions;

public class SigningException extends Exception {
	@Override
	public String getMessage() {
		return "The was an error while signing the message.";
	}
}
