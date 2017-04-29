package pt.tecnico.sec.dpm.client.register;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.crypto.SecretKey;

import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;

//Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;

public class ByzantineRegisterConnection {
	private final static int NONCE_SIZE = 64;

	private PrivateKey privateKey = null;
	private X509Certificate cert = null;
	private String url;
	private API port = null;
	private int counter = 0;
	
	public ByzantineRegisterConnection(String url) {
		this.url = url;
		
		// Creates the stub
		APIImplService service = null;
		
		try {
			service = new APIImplService(new URL(url));
		} catch (MalformedURLException e) {
			// It will not happen!
			e.printStackTrace();
		}
		
		port = service.getAPIImplPort();
	}
	
	public String getUrl() { return url; }
	
	public void init(PrivateKey privateKey, X509Certificate cert) throws AlreadyInitializedException {
		if(this.cert != null && this.privateKey != null)
			throw new AlreadyInitializedException();
		
		this.cert = cert;
		this.privateKey = privateKey;
	}
	
	
	// TODO: Check the methods below!!!
	
	
	public List<Object> write(int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, int wTS,
			byte[] cliSig) throws KeyConversionException, SigningException {
		//TODO: User tempCounter and only in the end update
		serverCounter ++;
		byte[] serverCounterBytes = (""+serverCounter).getBytes();
    	byte[] bonrr = "bonrr".getBytes();
    	byte[] write = "WRITE".getBytes();
    	byte[] sessionIDBytes = ("" + sessionID).getBytes();
    	byte[] cliCounterBytes = ("" + cliCounter).getBytes();
    	byte[] wTSBytes = SecurityFunctions.intToByteArray(wTS);
    	
    	
    	byte[] bytesToSign = SecurityFunctions.concatByteArrays(bonrr, write, serverCounterBytes, sessionIDBytes, cliCounterBytes, wTSBytes,
    			domain, username, password, cliSig);
    	
    	byte[] signedBytes = SecurityFunctions.makeDigitalSignature(privKey, bytesToSign);
    	
    	List<Object> result = port.deliverWrite(serverCounter, sessionID, cliCounter, domain, username, password, wTS, cliSig, signedBytes);
    	
		return result;
	}

	public List<Object> read(byte[] cliPublicKey, byte[] domain, byte[] username, byte[] cliSig) {
		serverCounter++;
		byte[] serverCounterBytes = SecurityFunctions.intToByteArray(serverCounter);
    	byte[] bonrr = "bonrr".getBytes();
    	byte[] write = "WRITE".getBytes();
    	
    	byte[] bytesToSign = SecurityFunctions.concatByteArrays(bonrr, write, serverCounterBytes, cliPublicKey, domain, username, cliSig);
		
		byte[] serverSig = null;
		
		List<Object> result = port.deliverRead(serverCounter, cliPublicKey, domain, username, serverSig);
		
		return result;
	}
}
