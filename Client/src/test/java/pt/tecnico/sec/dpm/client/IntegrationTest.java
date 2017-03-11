package pt.tecnico.sec.dpm.client;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.GivenAliasNotFoundException;
import pt.tecnico.sec.dpm.client.exceptions.NotInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.NullKeystoreElementException;
import pt.tecnico.sec.dpm.client.exceptions.WrongPasswordException;

import sun.security.x509.*;

public class IntegrationTest {
	public static DpmClient client = null;
	public static SecretKey symmKey = null;
	public static final char[] KEYSTORE_PASS = "ins3cur3".toCharArray();
	public static final char[] KEYS_PASS = "1nsecure".toCharArray();
	public static final String MY_NAME = "client";
	public static final String SYMM_NAME = "secretkey";
	
	// To capture the library output
	private final PrintStream standard = System.out;
	private ByteArrayOutputStream testOut;
	
	
	@BeforeClass
    public static void oneTimeSetUp() {
		KeyStore keystore = null;
		
        //generate and load keystore
        try {
        	keystore = KeyStore.getInstance("jceks");
		} catch (KeyStoreException e) {e.printStackTrace();}
        
        java.io.FileInputStream file = null;
        try {
        	file = new java.io.FileInputStream("keys/client/client.jks");
			keystore.load(file, KEYSTORE_PASS);
		} catch (NoSuchAlgorithmException e1) {	e1.printStackTrace();
		} catch (CertificateException e1) { e1.printStackTrace();
		} catch (IOException e1) { e1.printStackTrace();
		}finally {
	        if (file != null) {
	            try {
					file.close();
				} catch (IOException e) { e.printStackTrace();}
	        }
	    }
        
        //See if keystore already have a symmetric key, if don't add one
		try {symmKey =  (SecretKey) keystore.getKey("secretkey", "1nsecure".toCharArray());
		} catch (KeyStoreException e1) {e1.printStackTrace();
		} catch (UnrecoverableKeyException e) { e.printStackTrace();
		} catch (NoSuchAlgorithmException e) { e.printStackTrace();}
		
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(KEYS_PASS);
		
		if (symmKey == null){
			KeyGenerator keyGenAES = null;
			try{
	    		keyGenAES = KeyGenerator.getInstance("AES");
	    	}catch(NoSuchAlgorithmException e){System.out.print(e.getMessage());}
			
	        keyGenAES.init(256);
	        symmKey = keyGenAES.generateKey();
	        
	        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(symmKey);
	        try {keystore.setEntry("secretkey", skEntry, protParam);
			} catch (KeyStoreException e) {e.printStackTrace();}
			
	        java.io.FileOutputStream file2 = null;
	        try {
	        	file2 = new java.io.FileOutputStream("keys/client/client.jks");
				keystore.store(file2, KEYSTORE_PASS);
			} catch (NoSuchAlgorithmException e1) {	e1.printStackTrace();
			} catch (CertificateException e1) { e1.printStackTrace();
			} catch (IOException e1) { e1.printStackTrace();
			} catch (KeyStoreException e) { e.printStackTrace();
			}finally {
		        if (file != null) {
		            try {
						file.close();
					} catch (IOException e) { e.printStackTrace();}
		        }
		    }
		}
    }
	
	@Before
    public void setUp() {
		KeyStore keystore = null;
		KeyStore.PrivateKeyEntry privKeyEntry = null;
		
		try {
			keystore = KeyStore.getInstance("jceks");
			keystore.load(null, null);
			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(KEYS_PASS);
			keystore.setEntry(SYMM_NAME, new KeyStore.SecretKeyEntry(symmKey), protParam);
			
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	    	keyGen.initialize(2048);
	    	KeyPair pair = keyGen.generateKeyPair();
			privKeyEntry = new KeyStore.PrivateKeyEntry(pair.getPrivate(), generateCertificate(pair));
			keystore.setEntry(MY_NAME, privKeyEntry, protParam);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
    	client = new DpmClient("http://localhost:8080/ws.API/endpoint");
    	
    	try {
			client.init(keystore, KEYSTORE_PASS, MY_NAME, SYMM_NAME, KEYS_PASS);
		} catch (AlreadyInitializedException | NullKeystoreElementException | GivenAliasNotFoundException
				| WrongPasswordException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	testOut = new ByteArrayOutputStream();
    	System.setOut(new PrintStream(testOut));
    }
	
	@After
    public void tearDown() {
		System.setOut(standard);
		
    	try {
			client.close();
		} catch (NotInitializedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	
	// TESTS
	
	
	/************
	 * REGISTER *
	 ************/
    @Test
    public void correctRegister() throws NotInitializedException { 
    	testOut.reset();
    	
    	client.register_user();
    	assertEquals("", testOut.toString());
    }
    
    @Test
    public void keyInUseRegister() throws NotInitializedException {
    	testOut.reset();
    	
    	client.register_user();
    	client.register_user();
    	assertEquals("Ignoring fault message...\nThe given Public Key is already being used.\n", testOut.toString());
    }
    
    // Given by the faculty
    private X509Certificate[] generateCertificate(KeyPair pair) throws Exception {
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + 365 * 86400000l); //1 year
        CertificateValidity interval = new CertificateValidity(from, to);
        X500Name owner = new X500Name("C=PT, ST=SEC");

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(new BigInteger(64, new SecureRandom())));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER,owner);
        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(pair.getPrivate(), "SHA1withRSA");
        algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(pair.getPrivate(), "SHA1withRSA");
        return new X509Certificate[]{cert};
    }
}
