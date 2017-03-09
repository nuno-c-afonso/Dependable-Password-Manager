package pt.tecnico.sec.dpm.client;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.xml.ws.BindingProvider;
import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;

import pt.tecnico.sec.dpm.client.exceptions.*;

// Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;
import pt.tecnico.sec.dpm.server.*;
import ws.handler.SignatureHandler;

public class DpmClient {
	
	private PublicKey publicKey = null;
	private SecretKey symmetricKey = null;

	private API port = null; 
	
	public DpmClient(String url) {
		// Creates the stub
		APIImplService service = new APIImplService();
		port = service.getAPIImplPort();
		
		// Handler stuff
		BindingProvider bindingProvider = (BindingProvider) port;
		Map<String, Object> requestContext = bindingProvider.getRequestContext();
		requestContext.put(ENDPOINT_ADDRESS_PROPERTY, url);
		//requestContext.put(SignatureHandler.MYNAME, "")
		
	}
	
	// It is assumed that all keys are protected by the same password
	public void init(KeyStore keystore, char[] passwordKeystore, String cliPairName,
			String symmName, String pubServerName, char[] passwordKeys)
		throws AlreadyInitializedException, NullKeystoreElementException,
		GivenAliasNotFoundException, WrongPasswordException {
		
		if(publicKey != null && symmetricKey != null)
			throw new AlreadyInitializedException();
		
		if(keystore == null || passwordKeys == null || passwordKeystore == null)
			throw new NullKeystoreElementException();
		
		try {
			if(!keystore.containsAlias(cliPairName) ||  !keystore.containsAlias(symmName) || !keystore.containsAlias(pubServerName))
				throw new GivenAliasNotFoundException();
			
			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(passwordKeystore);
			symmetricKey = (SecretKey) keystore.getKey(symmName, passwordKeys);
			KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) keystore.getEntry(cliPairName, protParam);
		    publicKey = pke.getCertificate().getPublicKey();
		    
		    // TODO: Add handler configuration keys
		    
		} catch(UnrecoverableEntryException e) {
			throw new WrongPasswordException();
		} catch(NoSuchAlgorithmException | KeyStoreException e) {
			publicKey = null;
			symmetricKey = null;
			e.printStackTrace();
		}
	}
	
	public void register_user() throws NotInitializedException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		try {
			port.register(publicKey.getEncoded());
		} catch (NullArgException_Exception | PublicKeyInUseException_Exception e) {
			
		}			
	}
	
	public void save_password(byte[] domain, byte[] username, byte[] password) throws NotInitializedException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		try {
			port.put(publicKey.getEncoded(), domain, username, password);
		} catch (NoPublicKeyException_Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	public byte[] retrieve_password(byte[] domain, byte[] username) throws NotInitializedException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		byte[] retrivedPassword = null;
		try {
			retrivedPassword = port.get(publicKey.getEncoded(), domain, username);
		} catch (NoPasswordException_Exception | NullArgException_Exception e) {
			System.out.println(e.getMessage());
		}
		return retrivedPassword;
	}
	
	public void close() throws NotInitializedException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		//Destroy Symmetric Key
		try {
			symmetricKey.destroy();
			if(symmetricKey.isDestroyed()) symmetricKey = null;
		} catch (DestroyFailedException e) {
			System.out.println(e.getMessage());
		}
	}
	
}
