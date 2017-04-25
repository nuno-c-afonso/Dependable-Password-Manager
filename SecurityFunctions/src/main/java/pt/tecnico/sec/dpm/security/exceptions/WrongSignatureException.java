package pt.tecnico.sec.dpm.security.exceptions;

public class WrongSignatureException extends Exception {
	@Override
	public String getMessage() {
		return "The was an error while verifying the signature.";
	}
}
