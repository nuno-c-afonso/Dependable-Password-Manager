package pt.tecnico.sec.dpm.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.X509Certificate;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.DestroyFailedException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;

import pt.tecnico.sec.dpm.client.exceptions.*;

// Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;
import ws.handler.SignatureHandler;

public class DpmClient {
	
	PublicKey publicKey = null;
	SecretKey symmetricKey = null;
	PrivateKey privateKey = null;
	X509Certificate cert = null;
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
		
		String modUrl = url.toLowerCase().replace('/','0');
		
		if(publicKey != null && symmetricKey != null)
			throw new AlreadyInitializedException();
		
		if(keystore == null || passwordKeys == null || passwordKeystore == null || cliPairName==null || symmName == null)
			throw new NullKeystoreElementException();
		
		try {
			if(!keystore.containsAlias(cliPairName) ||  !keystore.containsAlias(symmName) || !keystore.containsAlias(modUrl))
				throw new GivenAliasNotFoundException();
			
			cert = (X509Certificate) keystore.getCertificate(modUrl);
			
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
	
	
	public void register_user() throws NotInitializedException,
	PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException, HandlerException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		try {
			port.register(publicKey.getEncoded());
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		} catch (WebServiceException e) {
			checkWebServiceException(e);
		}
	}
	
	
	public void save_password(byte[] domain, byte[] username, byte[] password)
			throws NotInitializedException, NullClientArgException, UnregisteredUserException, ConnectionWasClosedException, HandlerException {
		
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		if(domain == null || username == null || password == null)
			throw new NullClientArgException();
		
		try {
			byte[] iv = createIV(domain, username);
			
			port.put(publicKey.getEncoded(),
					 cipherWithSymmetric(symmetricKey, domain, iv), 
					 cipherWithSymmetric(symmetricKey,username, iv), 
					 cipherWithSymmetric(symmetricKey, password, iv));
			
		} catch (NoPublicKeyException_Exception e) {
			throw new UnregisteredUserException();
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		} catch (WebServiceException e) {
			checkWebServiceException(e);
		}
	}
	
	
	
	public byte[] retrieve_password(byte[] domain, byte[] username)
			throws NotInitializedException, NoPasswordException_Exception, NullClientArgException, UnregisteredUserException, ConnectionWasClosedException, HandlerException {
		
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		if(domain == null || username == null)
			throw new NullClientArgException();
		
		byte[] retrivedPassword = null;
		try {
			byte[] iv = createIV(domain, username);
			
			retrivedPassword = port.get(publicKey.getEncoded(), 
							   			cipherWithSymmetric(symmetricKey, domain, iv),
							   			cipherWithSymmetric(symmetricKey,username, iv));
			
			retrivedPassword = decipherWithSymmetric(symmetricKey,retrivedPassword, iv);
		} catch(NoPublicKeyException_Exception e) {
			throw new UnregisteredUserException();
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		} catch (WebServiceException e) {
			checkWebServiceException(e);
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
	
	public byte[] cipherWithSymmetric(SecretKey key, byte[] data, byte[] iv){
		byte[] returnData = null;
		try {
	        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        c.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(iv));
	        returnData = c.doFinal(data);
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }

		return returnData;		
	}
	
	public byte[] decipherWithSymmetric(SecretKey key, byte[] ecryptedData,byte[] iv) {
		byte[] returnData = null;
		try {
	        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        c.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(iv) );
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
		requestContext.put(SignatureHandler.PUBLICKEY, publicKey);
		requestContext.put(SignatureHandler.SERVERCERT, cert);
	}
	
	// To see what to do when getting a WebServiceException
	private void checkWebServiceException(WebServiceException e) throws ConnectionWasClosedException, HandlerException {
		Throwable cause = e.getCause();

        while (cause != null) {
            if (cause instanceof ConnectException)
                throw new ConnectionWasClosedException();

            cause = cause.getCause();
        }
        
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        
        throw new HandlerException(errors.toString());
	}
	
	private byte[] createIV(byte[] domain, byte[] username) {		
		byte[] result = new byte[16];
		byte[] bytesKey = publicKey.getEncoded();
		byte[] toHash = new byte[bytesKey.length + domain.length + username.length];
		
		System.arraycopy(bytesKey, 0, toHash, 0, bytesKey.length);
		System.arraycopy(domain, 0, toHash, bytesKey.length, domain.length);
		System.arraycopy(username, 0, toHash, bytesKey.length + username.length, username.length);
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(toHash);
			System.arraycopy(hash, 0, result, 0, 16);
		} catch (NoSuchAlgorithmException e) {
			// It should not happen
			e.printStackTrace();
		}
		
		return result;
	}
}
