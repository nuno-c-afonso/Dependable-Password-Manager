package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class GivenAliasNotFoundException extends Exception {
	@Override
	public String getMessage() {
		return "The Keystore alias were not found.";
	}
}
