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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.security.auth.DestroyFailedException;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceException;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;

import pt.tecnico.sec.dpm.client.exceptions.*;
import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
// Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;
import ws.handler.SignatureHandler;

public class DpmClient {
	private final static int NONCE_SIZE = 64;
	
	PublicKey publicKey = null;
	SecretKey symmetricKey = null;
	PrivateKey privateKey = null;
	X509Certificate cert = null;
	String url;
	int sessionID = -1;
	int counter = 0;
	int writeTS = 0;
	private API port = null;
	
	public DpmClient(String url) {
		// Creates the stub
		APIImplService service = new APIImplService();
		port = service.getAPIImplPort();
		this.url= url;
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
		    sessionID = -1;
		    
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
	
	
	public void register_user() throws NotInitializedException, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException,
	HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception,
	WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		try {
			byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
					SecurityFunctions.concatByteArrays("register".getBytes(), publicKey.getEncoded()));
			try {				
				sig = port.register(publicKey.getEncoded(), sig);
				SecurityFunctions.checkSignature(cert.getPublicKey(),
						SecurityFunctions.concatByteArrays("register".getBytes(), publicKey.getEncoded()), sig);
				
			} catch(PublicKeyInUseException_Exception e) {
				// Continues execution
			}
			
			// Asks the server for a new valid sessionID
			boolean cont = true;
			SecureRandom sr = null;
			
			try {
				sr = SecureRandom.getInstance("SHA1PRNG");
			} catch(NoSuchAlgorithmException nsae) {
				// It should not happen!
				nsae.printStackTrace();
			}
			
			byte[] nonce = new byte[NONCE_SIZE];
			List<Object> result = null;
			
			while(cont) {
				sr.nextBytes(nonce);

				try {					
					sig = SecurityFunctions.makeDigitalSignature(privateKey,
							SecurityFunctions.concatByteArrays("login".getBytes(), publicKey.getEncoded(), nonce));
					
					result = port.login(publicKey.getEncoded(), nonce, sig);					
					cont = false;
				} catch(DuplicatedNonceException_Exception dne) { /* Continue trying to connect */ }
			}

			byte[] serverNonce = (byte[]) result.get(0);
			int sessionID = (int) result.get(1);
			sig = (byte[]) result.get(2);
			
			nonce = SecurityFunctions.intToByteArray(SecurityFunctions.byteArrayToInt(nonce) + 1);
			
			if(!Arrays.equals(nonce, serverNonce))
				throw new WrongNonceException();
			
			SecurityFunctions.checkSignature(cert.getPublicKey(),
					SecurityFunctions.concatByteArrays("login".getBytes(), nonce, ("" + sessionID).getBytes()), sig);
			
			this.sessionID = sessionID;
			counter = 0;
			writeTS = 0;
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		} catch (WebServiceException e) {
			checkWebServiceException(e);
		}
	}
	
	
	public void save_password(byte[] domain, byte[] username, byte[] password)
			throws NotInitializedException, NullClientArgException, UnregisteredUserException,
			ConnectionWasClosedException, HandlerException, SigningException,
			KeyConversionException_Exception, SessionNotFoundException_Exception,
			SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {
		
// TODO: For testing purposes
//System.out.println("WRITE TIMESTAMP:" + writeTS);
		
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		if(sessionID < 0)
			throw new UnregisteredUserException();
		
		if(domain == null || username == null || password == null)
			throw new NullClientArgException();
		
		try {
			byte[] iv = createIV(domain, username);
			byte[] cDomain = cipherWithSymmetric(symmetricKey, domain, iv);
			byte[] cUsername = cipherWithSymmetric(symmetricKey,username, iv);
			byte[] cPassword = cipherWithSymmetric(symmetricKey, password, iv);
			
			int tmpCounter = counter + 1;
			byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
					SecurityFunctions.concatByteArrays("put".getBytes(), ("" + sessionID).getBytes(),
							("" + tmpCounter).getBytes(), cDomain, cUsername, cPassword, ("" + writeTS).getBytes()));
			
			List<Object> result = port.put(sessionID, tmpCounter, cDomain, cUsername, cPassword, writeTS, sig);
			
			sig = (byte[]) result.get(1);
			tmpCounter++;
			
			SecurityFunctions.checkSignature(cert.getPublicKey(),
					SecurityFunctions.concatByteArrays("put".getBytes(), ("" + sessionID).getBytes(), ("" + tmpCounter).getBytes()),
					sig);
			
			counter = tmpCounter;
			writeTS ++;
			
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
			throws NotInitializedException, NoPasswordException_Exception, NullClientArgException,
			UnregisteredUserException, ConnectionWasClosedException, HandlerException, SigningException,
			KeyConversionException_Exception, SessionNotFoundException_Exception, SigningException_Exception,
			WrongSignatureException_Exception, WrongSignatureException {
		
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		if(sessionID < 0)
			throw new UnregisteredUserException();
		
		if(domain == null || username == null)
			throw new NullClientArgException();
		
		byte[] retrivedPassword = null;
		try {
			byte[] iv = createIV(domain, username);
			byte[] cDomain = cipherWithSymmetric(symmetricKey, domain, iv);
			byte[] cUsername = cipherWithSymmetric(symmetricKey,username, iv);
			
			int tmpCounter = counter + 1;
			byte[] sig = SecurityFunctions.makeDigitalSignature(privateKey,
					SecurityFunctions.concatByteArrays("get".getBytes(), ("" + sessionID).getBytes(),
							("" + tmpCounter).getBytes(), cDomain, cUsername));
			
			List<Object> result = port.get(sessionID, tmpCounter, cDomain, cUsername,sig);
			
			// TODO: Check for null pointers and casts [IN ALL OF THE SERVER METHODS]!!!
			
			// Parsing the server result
			int serverCounter = (int) result.get(0);
			retrivedPassword = (byte[]) result.get(1);
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
			
			retrivedPassword = decipherWithSymmetric(symmetricKey,retrivedPassword, iv);
			writeTS = serverTS;
			counter = tmpCounter;			
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
		System.arraycopy(username, 0, toHash, bytesKey.length + domain.length, username.length);
		
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
