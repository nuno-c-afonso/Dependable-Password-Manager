package pt.tecnico.sec.dpm.server.broadcastServer;


import java.security.PublicKey;
import java.util.List;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.broadcastClient.BroadcastClient;
import pt.tecnico.sec.dpm.server.db.DPMDB;
import pt.tecnico.sec.dpm.server.exceptions.*;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.broadcastServer.BroadcastAPI")
public class BroadcastServer implements BroadcastAPI {
	private DPMDB dbMan = null;
	String[] urls;
	BroadcastClient broadcastClient = null;
	
	public BroadcastServer(DPMDB dbMan, String[] urls) {
		this.dbMan = dbMan;
		this.urls = urls;
		
	}
	
	public BroadcastServer() {
	
	}
	
	@Override
	public void broadcastPut(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTS, 
			byte[] bdSig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException, ConnectionClosedException {
		
		if(deviceID == null || domain == null || username == null || password == null || bdSig == null)
			return;
			
			if(broadcastClient == null){
				broadcastClient = new BroadcastClient(urls);
			}
		
			byte[] publicKey = dbMan.pubKeyFromDeviceID(deviceID);
			
			try {
				PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
				// Checks the DB signature
				SecurityFunctions.checkSignature(pubKey,
						SecurityFunctions.concatByteArrays(deviceID, domain, username, password, ("" + wTS).getBytes()),
						bdSig);
			} catch(WrongSignatureException | KeyConversionException e) {
				return;
			}
			
		
			boolean ok =dbMan.put(publicKey, deviceID, domain, username, password, wTS, bdSig);
			if(ok) {
				broadcastClient.Broadcast(deviceID, domain, username, password, wTS, bdSig);
			}
	}
}
