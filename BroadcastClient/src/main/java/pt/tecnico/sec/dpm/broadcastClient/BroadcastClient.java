package pt.tecnico.sec.dpm.broadcastClient;

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

import pt.tecnico.sec.dpm.broadcastClient.exceptions.*;
import pt.tecnico.sec.dpm.broadcastClient.register.BnnarWriter;
import pt.tecnico.sec.dpm.broadcastClient.register.BonarWriter;
import pt.tecnico.sec.dpm.broadcastClient.register.BonrrWriter;
import pt.tecnico.sec.dpm.broadcastClient.register.Writer;
import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;

// Classes generated from WSDL
import pt.tecnico.sec.dpm.server.*;

public class BroadcastClient {
	private Writer writer;
	
	public BroadcastClient(String[] urls, int numberOfFaults) {
		writer = new BnnarWriter(urls, numberOfFaults);
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
	
	public void register_user() throws Exception {
		writer.register_user();
	}
	
	public void save_password(byte[] domain, byte[] username, byte[] password) throws Exception {
		
		if(domain == null || username == null || password == null)
			throw new NullClientArgException();
		
		writer.put(domain, username, password);
	}
	
	public byte[] retrieve_password(byte[] domain, byte[] username) throws Exception {
		
		if(domain == null || username == null)
			throw new NullClientArgException();
		
		return writer.get(domain, username);
	}
	
	public void close() throws NotInitializedException {
		writer.close();
	}
}
