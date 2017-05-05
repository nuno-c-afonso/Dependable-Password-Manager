package pt.tecnico.sec.dpm.client.register;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
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
import javax.xml.ws.WebServiceException;

import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.ConnectionWasClosedException;
import pt.tecnico.sec.dpm.client.exceptions.HandlerException;
import pt.tecnico.sec.dpm.client.exceptions.NotInitializedException;
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
		
		try {
			APIImplService service = new APIImplService(new URL(url));
			port = service.getAPIImplPort();
		} catch (MalformedURLException e) {
			// It will not happen!
			e.printStackTrace();
		} catch (WebServiceException e) {
			if(!connectionWasClosed(e))
				throw e;
		}
	}
	
	public String getUrl() { return url; }
	
	public void init(PrivateKey privateKey, X509Certificate cert) throws AlreadyInitializedException {
		if(this.cert != null && this.privateKey != null)
			throw new AlreadyInitializedException();
		
		this.cert = cert;
		this.privateKey = privateKey;
	}
		
	public void register(PublicKey pubKey) throws SigningException, NullArgException_Exception,
	PublicKeyInvalidSizeException_Exception, NotInitializedException {
		
		if(port == null || privateKey == null || cert == null)
			throw new NotInitializedException();
		
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("register".getBytes(), pubKey.getEncoded()));
		
		try {
			sig = port.register(pubKey.getEncoded(), sig);
			
			if(sig == null)
				throw new WrongSignatureException();
			
			SecurityFunctions.checkSignature(cert.getPublicKey(),
					SecurityFunctions.concatByteArrays("register".getBytes(), pubKey.getEncoded()), sig);
		} catch(PublicKeyInUseException_Exception e) {
			// Ignore it!
		} catch(WrongSignatureException | KeyConversionException_Exception | SigningException_Exception
				| WrongSignatureException_Exception e) {
			register(pubKey);
		} catch(WebServiceException e) {
			if(!connectionWasClosed(e))
				throw e;
		}
	}
		
	public void login(PublicKey pubKey, byte[] deviceID) throws SigningException, NotInitializedException {
		if(port == null || privateKey == null || cert == null)
			throw new NotInitializedException();
		
		byte[] sig = null;
		
		SecureRandom sr = null;
		
    	try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch(NoSuchAlgorithmException nsae) {
			// It should not happen!
			nsae.printStackTrace();
		}
		
		nonce = new byte[NONCE_SIZE];
		boolean cont = true;
		
		while(cont) {
			sr.nextBytes(nonce);
			
			try {
				sig = SecurityFunctions.makeDigitalSignature(privateKey,
						SecurityFunctions.concatByteArrays("login".getBytes(), pubKey.getEncoded(), deviceID, nonce));
				
				sig = port.login(pubKey.getEncoded(), deviceID, nonce, sig);
				
				if(sig == null)
					throw new WrongSignatureException();
				
				SecurityFunctions.checkSignature(cert.getPublicKey(),
						SecurityFunctions.concatByteArrays("login".getBytes(), deviceID, nonce, ("1").getBytes()), sig);
				
				cont = false;
			} catch(DuplicatedNonceException_Exception e) {
				// Try again.
			} catch (KeyConversionException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoPublicKeyException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullArgException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SigningException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongSignatureException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongSignatureException e) {
				System.out.println("Wrong signature check on the Login!");
			} catch (WebServiceException e) {
				if(!connectionWasClosed(e))
					throw e;
			}
		}

		counter = 1;
	}
	
	public void put(byte[] deviceID, byte[] cDomain, byte[] cUsername, byte[] cPassword, int wTS, byte[] bdSig) throws UnregisteredUserException,
	SigningException, NoPublicKeyException_Exception, NullArgException_Exception, SessionNotFoundException_Exception, NotInitializedException {
		
		if(port == null || privateKey == null || cert == null)
			throw new NotInitializedException();
		
		if(nonce == null)
			throw new UnregisteredUserException();
		
		int tmpCounter = counter + 1;
		counter += 2;
		
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("put".getBytes(),
						deviceID,
						nonce,
						("" + tmpCounter).getBytes(),
						cDomain, cUsername, cPassword, ("" + wTS).getBytes(), bdSig));
		
		try {
			sig = port.put(deviceID, nonce, cDomain, cUsername, cPassword, wTS, bdSig, tmpCounter, sig);
			
			if(sig == null)
				throw new WrongSignatureException();
			
			tmpCounter++;
			SecurityFunctions.checkSignature(cert.getPublicKey(),
					SecurityFunctions.concatByteArrays("put".getBytes(), deviceID, nonce, ("" + tmpCounter).getBytes()), sig);
		} catch(WrongSignatureException | KeyConversionException_Exception | SigningException_Exception
				| WrongSignatureException_Exception e) {
			put(deviceID, cDomain, cUsername, cPassword, wTS, bdSig);
		} catch (WebServiceException e) {
			if(!connectionWasClosed(e))
				throw e;
		}
	}
	
	public List<Object> get(byte[] deviceID, byte[] domain, byte[] username) throws UnregisteredUserException, SigningException,
	NoPasswordException_Exception, NoPublicKeyException_Exception, NullArgException_Exception, SessionNotFoundException_Exception,
	NotInitializedException {
		
		if(port == null || privateKey == null || cert == null)
			throw new NotInitializedException();
		
		if(nonce == null)
			throw new UnregisteredUserException();
				
		int tmpCounter = counter + 1;
		counter += 2;
		
		byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
				SecurityFunctions.concatByteArrays("get".getBytes(), deviceID, nonce, ("" + tmpCounter).getBytes(), domain, username));
		
		try {
			List<Object> result = port.get(deviceID, nonce, domain, username, tmpCounter, sig);
			
			if(result == null || result.get(0) == null || result.get(2) == null || result.get(3) == null || result.get(4) == null)
				throw new WrongSignatureException();
			
			if(!(result.get(0) instanceof byte[] && result.get(1) instanceof Integer && result.get(2) instanceof byte[]
					&& result.get(3) instanceof byte[] && result.get(4) instanceof byte[]))
				throw new WrongSignatureException();
				
			// Parsing the server result
			byte[] retrivedPassword = (byte[]) result.get(0);
			int wTS = (int) result.get(1);
			byte[] deviceIDWr = (byte[]) result.get(2);
			byte[] clientSig = (byte[]) result.get(3);
			sig = (byte[]) result.get(4);
			tmpCounter ++;
			
			SecurityFunctions.checkSignature(cert.getPublicKey(),
					SecurityFunctions.concatByteArrays("get".getBytes(), deviceID, nonce,
							("" + tmpCounter).getBytes(), retrivedPassword, ("" + wTS).getBytes(), deviceIDWr, clientSig),
					sig);
			
			result = new ArrayList<Object>();
			result.add(retrivedPassword);
			result.add(wTS);
			result.add(deviceIDWr);
			result.add(clientSig);
			
			return result;
		} catch(WrongSignatureException | KeyConversionException_Exception | SigningException_Exception
				| WrongSignatureException_Exception e) {
			return get(deviceID, domain, username);
		} catch (WebServiceException e) {			
			if(!connectionWasClosed(e))
				throw e;
		}
		
		return null;
	}
	
	// To see what to do when getting a WebServiceException
	private boolean connectionWasClosed(WebServiceException e) {
		// All these exceptions are related to problems in the socket
		return true;
	}
}
