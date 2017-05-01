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
import pt.tecnico.sec.dpm.client.exceptions.ConnectionWasClosedException;
import pt.tecnico.sec.dpm.client.exceptions.GivenAliasNotFoundException;
import pt.tecnico.sec.dpm.client.exceptions.HandlerException;
import pt.tecnico.sec.dpm.client.exceptions.NotInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.NullClientArgException;
import pt.tecnico.sec.dpm.client.exceptions.NullKeystoreElementException;
import pt.tecnico.sec.dpm.client.exceptions.UnregisteredUserException;
import pt.tecnico.sec.dpm.client.exceptions.WrongNonceException;
import pt.tecnico.sec.dpm.client.exceptions.WrongPasswordException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.KeyConversionException_Exception;
import pt.tecnico.sec.dpm.server.NoPasswordException_Exception;
import pt.tecnico.sec.dpm.server.NoPublicKeyException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInUseException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInvalidSizeException;
import pt.tecnico.sec.dpm.server.PublicKeyInvalidSizeException_Exception;
import pt.tecnico.sec.dpm.server.SessionNotFoundException_Exception;
import pt.tecnico.sec.dpm.server.SigningException_Exception;
import pt.tecnico.sec.dpm.server.WrongSignatureException_Exception;
import sun.security.x509.*;

public class IntegrationTest {
	public static DpmClient client = null;
	public static SecretKey symmKey = null;
	public static X509Certificate cert[] = new X509Certificate[4];
	public static final char[] KEYSTORE_PASS = "ins3cur3".toCharArray();
	public static final char[] KEYS_PASS = "1nsecure".toCharArray();
	public static final String MY_NAME = "client";
	public static final String SYMM_NAME = "secretkey";
	public static final byte[] DOMAIN = "domain".getBytes();
	public static final byte[] USERNAME = "username".getBytes();
	public static final byte[] ORIGINAL_PASS = "pass".getBytes();
	public static final byte[] CHANGED_PASS = "pass2".getBytes();
	public static final String[] SERVER_ADDRS = {"http://localhost:8080/ws.API/endpoint",
    		"http://localhost:8081/ws.API/endpoint",
    		"http://localhost:8082/ws.API/endpoint",
    		"http://localhost:8083/ws.API/endpoint"};
	
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
        	file = new java.io.FileInputStream("../keys/client/client.jks");
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
        
		try {
			symmKey =  (SecretKey) keystore.getKey("secretkey", "1nsecure".toCharArray());
			
			for(int i = 0; i < SERVER_ADDRS.length; i++)
				cert[i] = (X509Certificate) keystore.getCertificate(SERVER_ADDRS[i].toLowerCase().replace('/', '0'));
		} catch (KeyStoreException e1) {e1.printStackTrace();
		} catch (UnrecoverableKeyException e) { e.printStackTrace();
		} catch (NoSuchAlgorithmException e) { e.printStackTrace();}
		
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
			
			for(int i = 0; i < SERVER_ADDRS.length; i++)
				keystore.setEntry(SERVER_ADDRS[i].toLowerCase().replace('/', '0'), new KeyStore.TrustedCertificateEntry(cert[i]), null);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
    	client = new DpmClient(SERVER_ADDRS, 1);
    	
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
    public void correctRegister()
    		throws NotInitializedException, PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException { 
    	
    	testOut.reset();
    	
    	client.register_user();
    	assertEquals("", testOut.toString());
    }
    
    @Test
    public void newSessionRegister()
    		throws NotInitializedException, PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException {
    	
    	client.register_user();
    	client.register_user();
    }
    
    // Needs to create a new client, with a key size bigger than allowed
    @Test(expected=PublicKeyInvalidSizeException_Exception.class)
    public void keyTooBigRegister()
    		throws NotInitializedException, PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException {
    	
    	KeyStore keystore = null;
		KeyStore.PrivateKeyEntry privKeyEntry = null;
		
		try {
			keystore = KeyStore.getInstance("jceks");
			keystore.load(null, null);
			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(KEYS_PASS);
			keystore.setEntry(SYMM_NAME, new KeyStore.SecretKeyEntry(symmKey), protParam);
			
			for(int i = 0; i < SERVER_ADDRS.length; i++)
				keystore.setEntry(SERVER_ADDRS[i].toLowerCase().replace('/', '0'), new KeyStore.TrustedCertificateEntry(cert[i]), null);
			
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
	    	
			// Increased the size to one not allowed
			keyGen.initialize(6144);
			
	    	KeyPair pair = keyGen.generateKeyPair();
			privKeyEntry = new KeyStore.PrivateKeyEntry(pair.getPrivate(), generateCertificate(pair));
			keystore.setEntry(MY_NAME, privKeyEntry, protParam);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
    	client = new DpmClient(SERVER_ADDRS, 1);
    	
    	try {
			client.init(keystore, KEYSTORE_PASS, MY_NAME, SYMM_NAME, KEYS_PASS);
		} catch (AlreadyInitializedException | NullKeystoreElementException | GivenAliasNotFoundException
				| WrongPasswordException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	client.register_user();
    }
    
    
    /********
	 * SAVE *
	 ********/
    @Test
    public void correctInsertSave()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException, UnregisteredUserException, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	testOut.reset();
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    	assertEquals("", testOut.toString());
    }
    
    @Test
    public void correctUpdateSave()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException, UnregisteredUserException, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	testOut.reset();
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    	client.save_password(DOMAIN, USERNAME, CHANGED_PASS);
    	assertEquals("", testOut.toString());
    }
    
    @Test(expected=UnregisteredUserException.class)
    public void unregisteredSave() throws NotInitializedException, NullClientArgException, UnregisteredUserException, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {    	
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    }
    
    @Test(expected=NullClientArgException.class)
    public void nullDomainSave()
    		throws NotInitializedException, NullClientArgException, UnregisteredUserException,
    		PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	client.register_user();
    	client.save_password(null, USERNAME, ORIGINAL_PASS);
    }
    
    @Test(expected=NullClientArgException.class)
    public void nullUsernameSave()
    		throws NotInitializedException, NullClientArgException, UnregisteredUserException,
    		PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	client.register_user();
    	client.save_password(DOMAIN, null, ORIGINAL_PASS);
    }
    
    @Test(expected=NullClientArgException.class)
    public void nullPassSave()
    		throws NotInitializedException, NullClientArgException, UnregisteredUserException,
    		PublicKeyInUseException_Exception, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, null);
    }
    
    
    /************
	 * RETRIEVE *
	 ************/
    @Test
    public void correctInsertRetrieve()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException,
    		UnregisteredUserException, NoPasswordException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	testOut.reset();
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    	byte[] pass = client.retrieve_password(DOMAIN, USERNAME);
    	
    	assertArrayEquals(ORIGINAL_PASS, pass);
    	assertEquals("", testOut.toString());
    }
    
    @Test
    public void correctUpdateRetrieve()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException,
    		UnregisteredUserException, NoPasswordException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	testOut.reset();
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    	client.save_password(DOMAIN, USERNAME, CHANGED_PASS);
    	byte[] pass = client.retrieve_password(DOMAIN, USERNAME);
    	
    	assertArrayEquals(CHANGED_PASS, pass);
    	assertEquals("", testOut.toString());
    }
    
    @Test(expected=UnregisteredUserException.class)
    public void unregisteredRetrieve()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException,
    		UnregisteredUserException, NoPasswordException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException {
    	
    	client.retrieve_password(DOMAIN, "noone".getBytes());
    }
    
    @Test(expected=NoPasswordException_Exception.class)
    public void noMatchingPassRetrieve()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException,
    		UnregisteredUserException, NoPasswordException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    	client.retrieve_password(DOMAIN, "noone".getBytes());
    }
    
    @Test(expected=NullClientArgException.class)
    public void nullDomainRetrieve()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException,
    		UnregisteredUserException, NoPasswordException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    	client.retrieve_password(null, USERNAME);
    }
    
    @Test(expected=NullClientArgException.class)
    public void nullUsernameRetrieve()
    		throws NotInitializedException, PublicKeyInUseException_Exception,
    		PublicKeyInvalidSizeException_Exception, NullClientArgException,
    		UnregisteredUserException, NoPasswordException_Exception, ConnectionWasClosedException, HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception, WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException, SessionNotFoundException_Exception {
    	
    	client.register_user();
    	client.save_password(DOMAIN, USERNAME, ORIGINAL_PASS);
    	client.retrieve_password(DOMAIN, null);
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
