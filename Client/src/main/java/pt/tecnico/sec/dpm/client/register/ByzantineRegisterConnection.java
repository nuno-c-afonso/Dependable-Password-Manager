package pt.tecnico.sec.dpm.client.register;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.PublicKey;
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
	
	public int login(PublicKey pubKey, byte[] deviceID, byte[] nonce) throws SigningException {
		int wTS = -1;
		List<Object> result;
		
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("login".getBytes(), pubKey.getEncoded(), nonce));
		
		result = port.login(pubKey.getEncoded(), deviceID, nonce, sig);
		
		// TODO: Extract the remaining of the information, to properly verify the signature
		byte[] serverNonce = (byte[]) result.get(1);
		sig = (byte[]) result.get(2);
		
		nonce = SecurityFunctions.intToByteArray(SecurityFunctions.byteArrayToInt(nonce) + 1);
		
		if(!Arrays.equals(nonce, serverNonce))
			throw new WrongNonceException();
		
		// TODO: It will be expected a counter with 0, not an incremented nonce!!!
		SecurityFunctions.checkSignature(cert.getPublicKey(),
				SecurityFunctions.concatByteArrays("login".getBytes(), nonce, ("" + sessionID).getBytes()), sig);

		counter = 0;
		
		// TODO: Only make this assignment after checking the right signature + that the last right information is correct!!!
		wTS = (int) result.get(0);
		
		return wTS;
	}
	
	public void put(byte[] cDomain, byte[] cUsername, byte[] cPassword, int wTS) throws UnregisteredUserException,
	SigningException, KeyConversionException_Exception, NoPublicKeyException_Exception, NullArgException_Exception,
	SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {
		
		if(sessionID < 0 || nonce == null)
			throw new UnregisteredUserException();
		
		// TODO: The freshness should be guaranteed by the combination of the nonce + counter!!!
		
		int tmpCounter = counter + 1;
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("put".getBytes(), ("" + sessionID).getBytes(),
						("" + tmpCounter).getBytes(), cDomain, cUsername, cPassword, ("" + wTS).getBytes()));
		
		List<Object> result = port.put(sessionID, tmpCounter, cDomain, cUsername, cPassword, wTS, sig);
		
		sig = (byte[]) result.get(1);
		tmpCounter++;
		
		SecurityFunctions.checkSignature(cert.getPublicKey(),
				SecurityFunctions.concatByteArrays("put".getBytes(), ("" + sessionID).getBytes(), ("" + tmpCounter).getBytes()),
				sig);
		
		counter = tmpCounter;
	}
	
	public List<Object> get(byte[] domain, byte[] username) throws UnregisteredUserException, SigningException,
	KeyConversionException_Exception, NoPasswordException_Exception, NoPublicKeyException_Exception,
	NullArgException_Exception, SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {
	
		if(sessionID < 0 || nonce == null)
			throw new UnregisteredUserException();
		
		int tmpCounter = counter + 1;
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("get".getBytes(), ("" + sessionID).getBytes(),
						("" + tmpCounter).getBytes(), domain, username));
		
		List<Object> result = port.get(sessionID, tmpCounter, domain, username, sig);
		
		// Parsing the server result
		int serverCounter = (int) result.get(0);
		byte[] retrivedPassword = (byte[]) result.get(1);
		int serverTS = (int) result.get(2);
		int writeCounter = (int) result.get(3);
		byte[] clientSig = (byte[]) result.get(4);
		sig = (byte[]) result.get(5);
		tmpCounter ++;
		
		// TODO: Check if need to do the additional verifications here!!!
		
		SecurityFunctions.checkSignature(cert.getPublicKey(),
				SecurityFunctions.concatByteArrays("get".getBytes(), ("" + sessionID).getBytes(),
						("" + tmpCounter).getBytes(), retrivedPassword, ("" + serverTS).getBytes(),
						("" + writeCounter).getBytes(), clientSig),
				sig);
		
		counter = tmpCounter;
		
		result = new ArrayList<Object>();
		result.add(retrivedPassword);
		result.add(serverTS);
		result.add(writeCounter);
		result.add(clientSig);
		
		return result;
	}
}
