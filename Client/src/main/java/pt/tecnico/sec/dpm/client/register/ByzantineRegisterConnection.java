package pt.tecnico.sec.dpm.client.register;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;

import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.UnregisteredUserException;
import pt.tecnico.sec.dpm.client.exceptions.WrongNonceException;
import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
//Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;

public class ByzantineRegisterConnection {
	private final static int NONCE_SIZE = 64;
	
	private PrivateKey privateKey = null;
	private X509Certificate cert = null;
	private String url;
	private API port = null;
	
	private byte[] nonce = null;
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
	
	public void register(PublicKey pubKey) throws SigningException, WrongSignatureException,
	KeyConversionException_Exception, NullArgException_Exception, PublicKeyInvalidSizeException_Exception,
	SigningException_Exception, WrongSignatureException_Exception {
		
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("register".getBytes(), pubKey.getEncoded()));
		
		try {
			sig = port.register(pubKey.getEncoded(), sig);
			
			SecurityFunctions.checkSignature(cert.getPublicKey(),
					SecurityFunctions.concatByteArrays("register".getBytes(), pubKey.getEncoded()), sig);
		} catch(PublicKeyInUseException_Exception e) {
			// Ignore it!
		}
	}
	
	public int login(PublicKey pubKey, byte[] deviceID) throws SigningException {
		int wTS = -1;
		byte[] sig = null;
		
		SecureRandom sr = null;
		
    	try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch(NoSuchAlgorithmException nsae) {
			// It should not happen!
			nsae.printStackTrace();
		}
		
		byte[] nonce = new byte[NONCE_SIZE];
		boolean cont = true;
		
		while(cont) {
			sr.nextBytes(nonce);
			
			try {
				sig = SecurityFunctions.makeDigitalSignature(privateKey,
						SecurityFunctions.concatByteArrays("login".getBytes(), pubKey.getEncoded(), deviceID, nonce));
				
				sig = port.login(pubKey.getEncoded(), deviceID, nonce, sig);
				
				cont = false;
			} catch(DuplicatedNonceException_Exception e) {
				// Try again.
			}
		}
		
		// TODO: Extract the remaining of the information, to properly verify the signature
		// TODO: Get the current wTS and the proof of it
		
		SecurityFunctions.checkSignature(cert.getPublicKey(),
				SecurityFunctions.concatByteArrays("login".getBytes(), deviceID, nonce, ("1").getBytes()), sig);

		counter = 1;
		
		// TODO: Only make this assignment after checking the right signature + that the last right information is correct!!!
		//		wTS = (int) result.get(0);
		
		return wTS;
	}
	
	public void put(byte[] deviceID, byte[] cDomain, byte[] cUsername, byte[] cPassword, int wTS, byte[] bdSig) throws UnregisteredUserException,
	SigningException, KeyConversionException_Exception, NoPublicKeyException_Exception, NullArgException_Exception,
	SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {
		
		if(nonce == null)
			throw new UnregisteredUserException();
		
		int tmpCounter = counter + 1;
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("put".getBytes(),
						deviceID,
						nonce,
						("" + tmpCounter).getBytes(),
						cDomain, cUsername, cPassword, ("" + wTS).getBytes(), bdSig));
		
		sig = port.put(deviceID, nonce, cDomain, cUsername, cPassword, wTS, bdSig, sig);
		
		tmpCounter++;
		SecurityFunctions.checkSignature(cert.getPublicKey(),
				SecurityFunctions.concatByteArrays("put".getBytes(), deviceID, nonce, ("" + tmpCounter).getBytes()), sig);
		
		counter = tmpCounter;
	}
	
	public List<Object> get(byte[] deviceID, byte[] domain, byte[] username) throws UnregisteredUserException, SigningException,
	KeyConversionException_Exception, NoPasswordException_Exception, NoPublicKeyException_Exception,
	NullArgException_Exception, SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {
		
		// TODO: Check what are the attribute references and if they have null values (here and every other function)
		
		if(nonce == null)
			throw new UnregisteredUserException();
				
		int tmpCounter = counter + 1;
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("get".getBytes(), deviceID, nonce, ("" + tmpCounter).getBytes(), domain, username));
		
		List<Object> result = port.get(deviceID, nonce, domain, username, sig);
		
		// Parsing the server result
		byte[] retrivedPassword = (byte[]) result.get(0);
		int wTS = (int) result.get(1);
		byte[] deviceIDWr = (byte[]) result.get(2);
		byte[] clientSig = (byte[]) result.get(3);
		sig = (byte[]) result.get(4);
		tmpCounter ++;
		
		// TODO: Check if need to do the additional verifications here!!!
		
		SecurityFunctions.checkSignature(cert.getPublicKey(),
				SecurityFunctions.concatByteArrays("get".getBytes(), deviceID, nonce,
						("" + tmpCounter).getBytes(), retrivedPassword, ("" + wTS).getBytes(), deviceIDWr, clientSig),
				sig);
		
		counter = tmpCounter;
		
		result = new ArrayList<Object>();
		result.add(retrivedPassword);
		result.add(wTS);
		result.add(deviceIDWr);
		result.add(clientSig);
		
		return result;
	}
}
