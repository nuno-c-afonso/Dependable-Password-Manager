package pt.tecnico.sec.dpm.client;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.MalformedURLException;
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
import java.util.List;
import java.net.URL;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.ws.WebServiceException;

import pt.tecnico.sec.dpm.client.exceptions.*;
import pt.tecnico.sec.dpm.client.register.BonrrWriter;
import pt.tecnico.sec.dpm.client.register.Writer;
import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;

// Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;

public class DpmClient {
	private Writer writer;
	
	public DpmClient(String[] urls, int numberOfFaults) {
		
		// TODO: Have a set of writers, not only the regular one!!!
		writer = new BonrrWriter(urls, numberOfFaults);
	}
	
	// It is assumed that all keys are protected by the same password
	public void init(KeyStore keystore, char[] passwordKeystore, String cliPairName,
			String symmName, char[] passwordKeys)
		throws AlreadyInitializedException, NullKeystoreElementException,
		GivenAliasNotFoundException, WrongPasswordException {
		
		if(keystore == null || passwordKeys == null || passwordKeystore == null || cliPairName==null || symmName == null)
			throw new NullKeystoreElementException();
		
		writer.initConns(keystore, passwordKeystore, cliPairName, symmName, passwordKeys);
	}
	
	public void register_user() throws NotInitializedException, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException,
	HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception,
	WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException {
		writer.register_user();
	}
	
	public void save_password(byte[] domain, byte[] username, byte[] password)
			throws NotInitializedException, NullClientArgException, UnregisteredUserException,
			ConnectionWasClosedException, HandlerException, SigningException,
			KeyConversionException_Exception, SessionNotFoundException_Exception,
			SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {
		
		if(domain == null || username == null || password == null)
			throw new NullClientArgException();
		
		writer.put(domain, username, password);
	}
	
	public byte[] retrieve_password(byte[] domain, byte[] username)
			throws NotInitializedException, NoPasswordException_Exception, NullClientArgException,
			UnregisteredUserException, ConnectionWasClosedException, HandlerException, SigningException,
			KeyConversionException_Exception, SessionNotFoundException_Exception, SigningException_Exception,
			WrongSignatureException_Exception, WrongSignatureException {
		
		if(domain == null || username == null)
			throw new NullClientArgException();
		
		return writer.get(domain, username);
	}
	
	/* TODO: Move this to the Writer
	public void close() throws NotInitializedException {
		if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		symmetricKey = null;					
		privateKey = null;
		publicKey = null;

	}
	*/
	
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
}
