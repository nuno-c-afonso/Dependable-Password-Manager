package pt.tecnico.sec.dpm.server.broadcastServer;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.exceptions.*;

@WebService
public interface BroadcastAPI {
	
	void broadcastPut(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException, 
			WrongSignatureException, SigningException, ConnectionClosedException;
}
