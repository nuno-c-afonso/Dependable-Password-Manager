package pt.tecnico.sec.dpm.server;

import pt.tecnico.sec.dpm.server.broadcast.BroadcastAPI;
import pt.tecnico.sec.dpm.server.broadcast.BroadcastServer;
import pt.tecnico.sec.dpm.server.broadcast.SignedEchoBroadcast;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import javax.jws.HandlerChain;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.API")
@HandlerChain(file = "/handler-chain.xml")
public class APIImpl implements API {
	// Size is given in bytes
	private static final int MAX_KEY_SIZE = 550;
	
	private DPMDB dbMan = null;
	private String url = null;
	private PrivateKey privKey = null;
	private HashMap<String, HashMap<String, Integer>> sessionCounters = null;
	private List<String> serverUrls;
	private Endpoint endpoint;
	
	// For testing purposes
	public APIImpl(String url, char[] keystorePass, char[] keyPass) throws NullArgException {
		init(url, keystorePass, keyPass);
		dbMan = new DPMDB();		
	}
	
	// For the byzantine servers
	public APIImpl(String url, char[] keystorePass, char[] keyPass, int index, List<String> serverUrls) throws NullArgException {
		init(url, keystorePass, keyPass);
		this.serverUrls = serverUrls;
		dbMan = new DPMDB(index);
		
		// Creates the entrypoint for the other servers to send the broadcast
		endpoint = Endpoint.create(new BroadcastServer(dbMan));
		URL toBroadcast = null;
		try {
			toBroadcast = new URL(url);
			toBroadcast = new URL(toBroadcast.getProtocol(), toBroadcast.getHost(), 20000 + index, "/ws.API/broadcast");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.printf("Starting broadcast service at %s%n", toBroadcast.toString());
		endpoint.publish(toBroadcast.toString());
	}
	
	private void init(String url, char[] keystorePass, char[] keyPass) throws NullArgException {
		if(url == null || keystorePass == null || keyPass == null)
			throw new NullArgException();
		
		sessionCounters = new HashMap<String, HashMap<String, Integer>>();
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
	public byte[] login(byte[] publicKey, byte[] deviceID, byte[] nonce, byte[] sig) throws SigningException,
	KeyConversionException, WrongSignatureException, NullArgException, NoPublicKeyException, DuplicatedNonceException {
		
		if(publicKey == null || deviceID == null || nonce == null || sig == null)
			throw new NullArgException();
		
		PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
		SecurityFunctions.checkSignature(pubKey, SecurityFunctions.concatByteArrays("login".getBytes(), publicKey, deviceID, nonce), sig);
		
		try {
			dbMan.login(publicKey, deviceID, nonce);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String deviceIDStr = Base64.getEncoder().encodeToString(deviceID);
		String nonceStr = Base64.getEncoder().encodeToString(nonce);
		if(!sessionCounters.containsKey(deviceIDStr))
			sessionCounters.put(deviceIDStr, new HashMap<String, Integer>());
		
		sessionCounters.get(deviceIDStr).put(nonceStr, 1);
		
		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey, 
				SecurityFunctions.concatByteArrays("login".getBytes(), deviceID, nonce, ("1").getBytes()));
		
		return serverSig;
	}

	@Override
	public byte[] put(byte[] deviceID, byte[] nonce, byte[] domain, byte[] username, byte[] password, int wTs, byte[] bdSig, byte[] sig)
			throws NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException {		
		
		if(deviceID == null || nonce == null || domain == null || username == null || password == null || bdSig == null || sig == null)
			throw new NullArgException();
		
		int matchingCounter = -1;
		String deviceIDStr = null;
		String nonceStr = null;
		
		//Start the Algorithm
		
		try {
			byte[] publicKey = dbMan.pubKeyFromDeviceID(deviceID);
			
			deviceIDStr = Base64.getEncoder().encodeToString(deviceID);
			nonceStr = Base64.getEncoder().encodeToString(nonce);
			
			if(sessionCounters.get(deviceIDStr) == null || !sessionCounters.get(deviceIDStr).containsKey(nonceStr)) {
				System.out.println("There was no counter for the given session!");
				throw new SessionNotFoundException();
			}
			
			// Checks message signature
			matchingCounter = sessionCounters.get(deviceIDStr).get(nonceStr) + 1;
			PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
			SecurityFunctions.checkSignature(pubKey,
					SecurityFunctions.concatByteArrays("put".getBytes(), deviceID, nonce, ("" + matchingCounter).getBytes(),
							domain, username, password, ("" + wTs).getBytes(), bdSig),
					sig);
			/*
			// Checks the DB signature
			SecurityFunctions.checkSignature(pubKey,
					SecurityFunctions.concatByteArrays(deviceID, domain, username, password, ("" + wTs).getBytes()),
					bdSig);
			
			dbMan.put(pubKey.getEncoded(), deviceID, domain, username, password, wTs, bdSig);
			*/
			
			put(deviceID, domain, username, password, wTs, bdSig);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
		int updateCounter = matchingCounter + 1;
		sessionCounters.get(deviceIDStr).put(nonceStr, updateCounter);				
		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey, SecurityFunctions.concatByteArrays("put".getBytes(),
				deviceID, nonce, ("" + updateCounter).getBytes()));

		return serverSig;
	}
	
	private boolean put(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTs, byte[] bdSig) 
			throws WrongSignatureException, KeyConversionException, SessionNotFoundException, ConnectionClosedException {	
	
		byte[] publicKey = dbMan.pubKeyFromDeviceID(deviceID);
					
		PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
		
		// Checks the DB signature
		SecurityFunctions.checkSignature(pubKey,
				SecurityFunctions.concatByteArrays(deviceID, domain, username, password, ("" + wTs).getBytes()),
				bdSig);
		
		boolean result = dbMan.put(pubKey.getEncoded(), deviceID, domain, username, password, wTs, bdSig);	
		System.out.println(result);
		
		return result;
	}
	
	
	
	// FIXME: Use locks for the counters!!!
	@Override
	public List<Object> get(byte[] deviceID, byte[] nonce, byte[] domain, byte[] username, byte[] sig)
			throws NoPasswordException, NullArgException, SessionNotFoundException,
			KeyConversionException, WrongSignatureException, SigningException {		
		
		if(deviceID == null || nonce == null || domain == null || username == null || sig == null)
			throw new NullArgException();
		
		List<Object> prevWrite = null;
		int matchingCounter = -1;
		String deviceIDStr = null;
		String nonceStr = null;
		
		try {
			byte[] publicKey = dbMan.pubKeyFromDeviceID(deviceID);
			
			deviceIDStr = Base64.getEncoder().encodeToString(deviceID);
			nonceStr = Base64.getEncoder().encodeToString(nonce);
			
			if(sessionCounters.get(deviceIDStr) == null || !sessionCounters.get(deviceIDStr).containsKey(nonceStr))
				throw new SessionNotFoundException();
			
			matchingCounter = sessionCounters.get(deviceIDStr).get(nonceStr) + 1;
			PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);			
			SecurityFunctions.checkSignature(pubKey, SecurityFunctions.concatByteArrays("get".getBytes(), deviceID, nonce,
					("" + matchingCounter).getBytes(), domain, username), sig);
			
			// Returns: [password, w_ts, device_id, cl_sig]
			prevWrite = dbMan.get(publicKey, domain, username);
		} catch (NoResultException nre) {
			throw new NoPasswordException();
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		int updateCounter = matchingCounter + 1;
		sessionCounters.get(deviceIDStr).put(nonceStr, updateCounter);	
		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey,
				SecurityFunctions.concatByteArrays("get".getBytes(),
				deviceID,
				nonce,
				("" + updateCounter).getBytes(),
				(byte[]) prevWrite.get(0),
				("" + prevWrite.get(1)).getBytes(),
				(byte[]) prevWrite.get(2),
				(byte[]) prevWrite.get(3)));
		
		prevWrite.add(serverSig);
		return prevWrite;
	}
	
	public void close() {
		dbMan.close();		
		privKey = null;
		
		if(endpoint != null)
			endpoint.stop();
		endpoint = null;
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
	public void insertSessionCounter(byte[] deviceID, byte[] nonce, int counter) {
		String deviceIDStr = Base64.getEncoder().encodeToString(deviceID);
		String nonceStr = Base64.getEncoder().encodeToString(nonce);
		
		sessionCounters.put(deviceIDStr, new HashMap<String, Integer>());
		sessionCounters.get(deviceIDStr).put(nonceStr, counter);
	}
}
