package pt.tecnico.sec.dpm.server;

import pt.tecnico.sec.dpm.server.db.*;
import pt.tecnico.sec.dpm.server.exceptions.*;
import ws.handler.ServerSignatureHandler;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

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
	
	@Resource
	private WebServiceContext webServiceContext;
	
	public APIImpl(String url, char[] keystorePass, char[] keyPass) throws NullArgException {
		if(url == null)
			throw new NullArgException();
		
		dbMan = new DPMDB();
		this.url = url.toLowerCase();
		this.url = this.url.replace('/', '0');
		
		retrievePrivateKey(keystorePass, keyPass);
	}
	
	// Used for the unit tests context
	public APIImpl() {
		dbMan = new DPMDB();
	}
	
	@Override
	public void register(byte[] publicKey) throws PublicKeyInUseException, NullArgException, PublicKeyInvalidSizeException {
		setMessageContext();
		
		try {
			dbMan.register(publicKey);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void put(byte[] publicKey, byte[] domain, byte[] username, byte[] password) throws NoPublicKeyException, NullArgException {
		setMessageContext();
		
		//Start the Algorithm
		
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
}
