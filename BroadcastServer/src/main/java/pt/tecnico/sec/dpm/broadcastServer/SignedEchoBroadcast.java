package pt.tecnico.sec.dpm.server.broadcast;


import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.ConnectionClosedException;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.NoPublicKeyException;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.NullArgException;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.SessionNotFoundException;

public interface SignedEchoBroadcast {
	
	//FIXME: Check this return
	void sendBroadcast(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTS, 
			byte[] bdSig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException, ConnectionClosedException;

}