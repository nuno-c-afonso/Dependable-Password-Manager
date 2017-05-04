package pt.tecnico.sec.dpm.broadcastServer;

import java.util.List;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.broadcastServer.db.DPMDB;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.ConnectionClosedException;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.NoPublicKeyException;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.NullArgException;
import pt.tecnico.sec.dpm.broadcastServer.exceptions.SessionNotFoundException;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.broadcastServer.BroadcastAPI")
public class BroadcastServer implements BroadcastAPI {
	private DPMDB dbMan = null;
	
	public BroadcastServer(DPMDB dbMan) {
		this.dbMan = dbMan;
	}
	
	public BroadcastServer() {
	
	}
	

	@Override
	public void broadcastPut(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTS, 
			byte[] bdSig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException, ConnectionClosedException {
			byte[] pubKey = dbMan.pubKeyFromDeviceID(deviceID);
			
			
			boolean ok =dbMan.put(pubKey, deviceID, domain, username, password, wTS, bdSig);
			if(ok) {
				//sendBroadcast(deviceID, domain, username, password, wTS, bdSig);
			}
	}
}
