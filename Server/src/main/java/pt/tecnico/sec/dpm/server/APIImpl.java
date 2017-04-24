package pt.tecnico.sec.dpm.server;

import pt.tecnico.sec.dpm.server.db.*;
import pt.tecnico.sec.dpm.server.exceptions.*;
import pt.tecnico.sec.dpm.security.*;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import ws.handler.ServerSignatureHandler;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.security.auth.DestroyFailedException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.API")
@HandlerChain(file = "/handler-chain.xml")
public class APIImpl implements API {  	
	// Size is given in bytes
	private static final int MAX_KEY_SIZE = 550;
	
	private DPMDB dbMan = null;
	private String url = null;
	private PrivateKey privKey = null;
	private HashMap<Integer, Integer> sessionCounters = null;
	
	public APIImpl(String url, char[] keystorePass, char[] keyPass) throws NullArgException {
		if(url == null)
			throw new NullArgException();
		
		dbMan = new DPMDB();
		sessionCounters = new HashMap<Integer, Integer>();
		this.url = url.toLowerCase();
		this.url = this.url.replace('/', '0');
		
		retrievePrivateKey(keystorePass, keyPass);
	}
	
	@Override
	public byte[] register(byte[] publicKey, byte[] sig) throws PublicKeyInUseException, NullArgException,
	PublicKeyInvalidSizeException, KeyConversionException, WrongSignatureException, SigningException {		
		
		if(publicKey == null || sig == null)
			throw new NullArgException();
		
		if(publicKey.length > MAX_KEY_SIZE)
			throw new PublicKeyInvalidSizeException();
		
		PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
		SecurityFunctions.checkSignature(pubKey, SecurityFunctions.concatByteArrays("register".getBytes(), publicKey), sig);
		
		try {
			dbMan.register(publicKey);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return SecurityFunctions.makeDigitalSignature(privKey, SecurityFunctions.concatByteArrays("register".getBytes(), publicKey));
	}
	
	@Override
	public List<Object> login(byte[] publicKey, byte[] nonce, byte[] sig) throws SigningException,
	KeyConversionException, WrongSignatureException, NullArgException, NoPublicKeyException, DuplicatedNonceException {
		PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
		SecurityFunctions.checkSignature(pubKey, SecurityFunctions.concatByteArrays("login".getBytes(), publicKey, nonce), sig);
		int sessionID = -1;
		
		try {
			sessionID = dbMan.login(publicKey, nonce);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sessionCounters.put(sessionID, 0);
		nonce = SecurityFunctions.intToByteArray(SecurityFunctions.byteArrayToInt(nonce) + 1);		
		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey, SecurityFunctions.concatByteArrays("login".getBytes(), nonce, ("" + sessionID).getBytes()));
		List<Object> res = new ArrayList<Object>();
		res.add(nonce);
		res.add(sessionID);
		res.add(serverSig);
		return res;
	}

	// FIXME: Use locks for the counters!!!
	@Override
	public List<Object> put(int sessionID, int counter, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig)
			throws NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException {		
		
		if(domain == null || username == null || password == null || sig == null)
			throw new NullArgException();
		
		int matchingCounter = -1;
		
		try {
			byte[] publicKey = dbMan.pubKeyFromSession(sessionID);
			
			matchingCounter = sessionCounters.get(sessionID) + 1;
			if(counter != matchingCounter)
				throw new WrongSignatureException();
			
			PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
			SecurityFunctions.checkSignature(pubKey,
					SecurityFunctions.concatByteArrays("put".getBytes(),("" + sessionID).getBytes(), ("" + matchingCounter).getBytes(),
							domain, username, password, ("" + wTs).getBytes()),
					sig);
			
			// FIXME: Make the needed checks for when updating (byzantine algorithms)!!!
			dbMan.put(sessionID, counter, domain, username, password, wTs, sig);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		int updateCounter = matchingCounter + 1;
		sessionCounters.put(sessionID, updateCounter);				
		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey, SecurityFunctions.concatByteArrays("put".getBytes(),
				("" + sessionID).getBytes(), ("" + updateCounter).getBytes()));
		
		List<Object> res = new ArrayList<Object>();
		res.add(updateCounter);
		res.add(serverSig);
		return res;
	}
	
	// FIXME: Use locks for the counters!!!
	@Override
	public List<Object> get(int sessionID, int counter, byte[] domain, byte[] username, byte[] sig)
			throws NoPasswordException, NullArgException, SessionNotFoundException,
			KeyConversionException, WrongSignatureException, SigningException {		
		
		if(domain == null || username == null || sig == null)
			throw new NullArgException();
		
		List<Object> prevWrite = null;
		int matchingCounter = -1;
		
		try {
			byte[] publicKey = dbMan.pubKeyFromSession(sessionID);
			matchingCounter = sessionCounters.get(sessionID) + 1;
			
			if(counter != matchingCounter)
				throw new WrongSignatureException();
			
			PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);			
			SecurityFunctions.checkSignature(pubKey, SecurityFunctions.concatByteArrays("get".getBytes(),("" + sessionID).getBytes(),
					("" + matchingCounter).getBytes(), domain, username), sig);
			
			// Returns: [password, w_ts, counter_ws, cl_sig]
			prevWrite = dbMan.get(publicKey, domain, username);
		} catch (NoResultException nre) {
			throw new NoPasswordException();
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		int updateCounter = matchingCounter + 1;
		sessionCounters.put(sessionID, updateCounter);				
		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey,
				SecurityFunctions.concatByteArrays("get".getBytes(),
				("" + sessionID).getBytes(),
				("" + updateCounter).getBytes(),
				(byte[]) prevWrite.get(0),
				("" + prevWrite.get(1)).getBytes(),
				("" + prevWrite.get(2)).getBytes(),
				(byte[]) prevWrite.get(3)));
		
		prevWrite.add(0, updateCounter);
		prevWrite.add(serverSig);
		return prevWrite;
	}
	
	public void close() {
		dbMan.close();		
		privKey = null;
	}
		
	private void retrievePrivateKey(char[] keystorePass, char[] keyPass) {
		// The password is the same as the one used on the clients
		FileInputStream file = null;
		KeyStore keystore = null;
		
		try {
			keystore = KeyStore.getInstance("jceks");
			file = new FileInputStream("../keys/" + url + "/" + url + ".jks");
			keystore.load(file, keystorePass);
			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keyPass);
			KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) keystore.getEntry(url, protParam);
		    privKey = pke.getPrivateKey();
			file.close();
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException
				| IOException | UnrecoverableEntryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Functions needed for testing
	public void insertSessionCounter(int session, int counter) {
		sessionCounters.put(session, counter);
	}
}
