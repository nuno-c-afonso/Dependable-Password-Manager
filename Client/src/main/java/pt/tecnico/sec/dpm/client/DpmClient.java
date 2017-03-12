package pt.tecnico.sec.dpm.client;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.xml.ws.BindingProvider;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;

import pt.tecnico.sec.dpm.client.exceptions.*;

// Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;
import ws.handler.SignatureHandler;

public class DpmClient {
	
	PublicKey publicKey = null;
	SecretKey symmetricKey = null;
	PrivateKey privateKey = null;
	String url;
	Map<String, Object> requestContext = null;

	private API port = null; 
	
	public DpmClient(String url) {
		// Creates the stub
		APIImplService service = new APIImplService();
		port = service.getAPIImplPort();
		this.url= url;
		
		// Handler stuff
		BindingProvider bindingProvider = (BindingProvider) port;
		requestContext = bindingProvider.getRequestContext();
	}
	
	// It is assumed that all keys are protected by the same password
	public void init(KeyStore keystore, char[] passwordKeystore, String cliPairName,
			String symmName, char[] passwordKeys)
		throws AlreadyInitializedException, NullKeystoreElementException,
		GivenAliasNotFoundException, WrongPasswordException {
		
		if(publicKey != null && symmetricKey != null)
			throw new AlreadyInitializedException();
		
		if(keystore == null || passwordKeys == null || passwordKeystore == null || cliPairName==null || symmName == null)
			throw new NullKeystoreElementException();
		
		try {
			if(!keystore.containsAlias(cliPairName) ||  !keystore.containsAlias(symmName))// || !keystore.containsAlias(url.toLowerCase().replace('/', '0')))
				throw new GivenAliasNotFoundException();
			
			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(passwordKeys);
			symmetricKey = (SecretKey) keystore.getKey(symmName, passwordKeys);
			KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) keystore.getEntry(cliPairName, protParam);
		    publicKey = pke.getCertificate().getPublicKey();
		    privateKey = pke.getPrivateKey();
		    
		    // Passes info to the handlers
		    setMessageContext(passwordKeystore, passwordKeys);
		    
		} catch(UnrecoverableEntryException e) {
			System.out.println(e.getMessage());
			System.out.println("erro a abrir chave 1");
			throw new WrongPasswordException();
		} catch(NoSuchAlgorithmException | KeyStoreException e) {
			System.out.println("erro a abrir chave 2");
			publicKey = null;
			symmetricKey = null;
			e.printStackTrace();
			
		}
	}
	
	
	//TODO: CIPHER INFORMATIONS
	// EVERYTHING EXCEPT PUBKEY
	public void register_user() throws NotInitializedException,
	PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		try {
			port.register(publicKey.getEncoded());
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		}
	}
	
	
	
	public void save_password(byte[] domain, byte[] username, byte[] password)
			throws NotInitializedException, NullClientArgException, UnregisteredUserException {
		
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		if(domain == null || username == null || password == null)
			throw new NullClientArgException();
		
		try {
			port.put(publicKey.getEncoded(), 
					 cipherWithSymmetric(symmetricKey, domain), 
					 cipherWithSymmetric(symmetricKey,username), 
					 cipherWithSymmetric(symmetricKey, password));
			
		} catch (NoPublicKeyException_Exception e) {
			throw new UnregisteredUserException();
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		}
	}
	
	
	
	public byte[] retrieve_password(byte[] domain, byte[] username)
			throws NotInitializedException, NoPasswordException_Exception, NullClientArgException, UnregisteredUserException {
		
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		if(domain == null || username == null)
			throw new NullClientArgException();
		
		byte[] retrivedPassword = null;
		try {
			retrivedPassword = port.get(publicKey.getEncoded(), 
							   			cipherWithSymmetric(symmetricKey, domain),
							   			cipherWithSymmetric(symmetricKey,username));
			
			retrivedPassword = decipherWithSymmetric(symmetricKey,retrivedPassword);
		} catch(NoPublicKeyException_Exception e) {
			throw new UnregisteredUserException();
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		}
		
		return retrivedPassword;
	}
	
	public void close() throws NotInitializedException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		symmetricKey = null;					
		privateKey = null;
		publicKey = null;

	}
	
	public byte[] cipherWithSymmetric(SecretKey key, byte[] data){
		byte[] returnData = null;
		try {
	        Cipher c = Cipher.getInstance("AES");
	        c.init(Cipher.ENCRYPT_MODE, key);
	         returnData = c.doFinal(data);
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }

		return returnData;		
	}
	
	public byte[] decipherWithSymmetric(SecretKey key, byte[] ecryptedData) {
		byte[] returnData = null;
		try {
	        Cipher c = Cipher.getInstance("AES");
	        c.init(Cipher.DECRYPT_MODE, key);
	         returnData = c.doFinal(ecryptedData);
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }

		return returnData;
	}
	
	private void setMessageContext(char[] passwordKeystore, char[] passwordKeys) {		
		requestContext.put(ENDPOINT_ADDRESS_PROPERTY, url);
		requestContext.put(SignatureHandler.MYNAME, "Client");
		requestContext.put(SignatureHandler.OTHERSNAME, url);
		requestContext.put(SignatureHandler.PRIVATEKEY, privateKey);
		requestContext.put(SignatureHandler.SYMMETRICKEY, symmetricKey);
		requestContext.put(SignatureHandler.PASSWORDKEYS, passwordKeys);
		requestContext.put(SignatureHandler.PASSWORDKEYSTORE, passwordKeystore);
	}
}
