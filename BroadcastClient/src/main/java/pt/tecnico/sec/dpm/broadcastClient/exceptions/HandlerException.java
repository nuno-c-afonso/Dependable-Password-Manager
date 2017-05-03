package pt.tecnico.sec.dpm.broadcastClient.exceptions;

public class HandlerException extends Exception {
	private String prevStackTrace;
	
	public HandlerException(String prevStackTrace) {
		this.prevStackTrace = prevStackTrace;
	}
	
	@Override
	public String getMessage() {
		return "There was an exception in a handler.\n" + prevStackTrace;
	}
}
