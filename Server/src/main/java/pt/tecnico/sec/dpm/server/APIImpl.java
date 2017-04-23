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
	private DPMDB dbMan = null;
	private String url = null;
	private PrivateKey privKey = null;
	private HashMap<byte[], Integer> sessionCounters = null;
	
	public APIImpl(String url, char[] keystorePass, char[] keyPass) throws NullArgException {
		if(url == null)
			throw new NullArgException();
		
		dbMan = new DPMDB();
		sessionCounters = new HashMap<byte[], Integer>();
		this.url = url.toLowerCase();
		this.url = this.url.replace('/', '0');
		
		retrievePrivateKey(keystorePass, keyPass);
	}
	
	@Override
	public byte[] register(byte[] publicKey, byte[] sig) throws PublicKeyInUseException, NullArgException,
	PublicKeyInvalidSizeException, KeyConversionException, WrongSignatureException, SigningException {		
		
		PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
		SecurityFunctions.checkSignature(pubKey, "register", sig);
		
		try {
			dbMan.register(publicKey);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return SecurityFunctions.makeDigitalSignature(privKey, concatByteArrays("register".getBytes(), publicKey));
	}
	
	@Override
	public List<byte[]> login(byte[] publicKey, byte[] nonce, byte[] sig) {
		PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
		SecurityFunctions.checkSignature(pubKey, concatByteArrays("login".getBytes(), publicKey, nonce), sig);
		byte[] sessionID = null;
		
		try {
			
			// TODO: Insert a new counter in this new session
			// TODO: Save the counter here (HashMap) or in the DB?
			sessionID = dbMan.login(publicKey, nonce);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		nonce = intToByteArray(byteArrayToInt(nonce) + 1);		
		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey, concatByteArrays("login".getBytes(), nonce, sessionID));
		return insertByteArraysOnList(nonce, sessionID, serverSig);
	}

	@Override
	public void put(byte[] publicKey, byte[] domain, byte[] username, byte[] password) throws NoPublicKeyException, NullArgException {
		setMessageContext();
		
		try {
			dbMan.put(publicKey, domain, username, password);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public byte[] get(byte[] publicKey, byte[] domain, byte[] username)
			throws NoPasswordException, NullArgException, NoPublicKeyException {
		
		setMessageContext();
		
		byte[] res = null;
		
		try {
			res = dbMan.get(publicKey, domain, username);
		} catch (NoResultException nre) {
			throw new NoPasswordException();
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return res;
	}
	
	public void close() {
		dbMan.close();		
		privKey = null;
	}
	
	// Needed for handler
	private void setMessageContext() {		
		MessageContext messageContext = webServiceContext.getMessageContext();
		messageContext.put(ServerSignatureHandler.MYNAME, url);
		messageContext.put(ServerSignatureHandler.PRIVATEKEY, privKey);
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
	
	// Some extra methods for type conversion
	private int byteArrayToInt(byte[] bytes) {
	     return new BigInteger(bytes).intValue();
	}
	
	private byte[] intToByteArray(int n) {
		int ARRAY_SIZE = 64;
		ByteBuffer b = ByteBuffer.allocate(ARRAY_SIZE + 1);
		b.putInt(n);
		return b.array();
	}
	
	private List<byte[]> insertByteArraysOnList(byte[]... arrays) {
		List<byte[]> lst = new ArrayList<byte[]>();
		
		for(byte[] el : arrays)
			lst.add(el);
		return lst;
	}
	
	private byte[] concatByteArrays(byte[]... arrays) {
		int newSize = 0;
		int counterSize = 0;
		
		for(byte[] el : arrays)
			newSize += el.length;
		
		byte[] result = new byte[newSize];
		for(byte[] el : arrays) {
			int elSize = el.length;
			System.arraycopy(el, 0, result, counterSize, elSize);
			counterSize += elSize;
		}
		
		return result;
	}
}
