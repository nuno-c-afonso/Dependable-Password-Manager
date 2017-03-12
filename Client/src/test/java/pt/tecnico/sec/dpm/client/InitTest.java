package pt.tecnico.sec.dpm.client;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import pt.tecnico.sec.dpm.client.DpmClient;
import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.GivenAliasNotFoundException;
import pt.tecnico.sec.dpm.client.exceptions.NullKeystoreElementException;
import pt.tecnico.sec.dpm.client.exceptions.WrongPasswordException;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.EnumSet;
import java.util.Enumeration;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
public class InitTest {

    // static members
	public static PublicKey publicKey = null;	
	public static PrivateKey privateKey = null;
	public static SecretKey symmetricKey = null;	
	public static DpmClient client = null;
	public static KeyStore keystore = null; 
	public static int SYMM_KEY_SIZE = 192;
	
    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {
    	     
        
        //generate and load keystore
        try {
        	keystore = KeyStore.getInstance("jceks");
		} catch (KeyStoreException e) {e.printStackTrace();}
        
        java.io.FileInputStream file = null;
        char[] passwordFile = "ins3cur3".toCharArray();
        try {
        	file = new java.io.FileInputStream("../keys/client/client.jks");
			keystore.load(file,passwordFile);
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
		try {symmetricKey =  (SecretKey) keystore.getKey("secretkey", "1nsecure".toCharArray());
		} catch (KeyStoreException e1) {e1.printStackTrace();
		} catch (UnrecoverableKeyException e) { e.printStackTrace();
		} catch (NoSuchAlgorithmException e) { e.printStackTrace();}
		
		
		
		char[] pass = "1nsecure".toCharArray();
        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pass);
		
		
		if (symmetricKey == null){
			KeyGenerator keyGenAES = null;
			try{
	    		keyGenAES = KeyGenerator.getInstance("AES");
	    	}catch(NoSuchAlgorithmException e){System.out.print(e.getMessage());}
			
	        keyGenAES.init(SYMM_KEY_SIZE);
	        symmetricKey = keyGenAES.generateKey();
	        
	        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(symmetricKey);
	        try {keystore.setEntry("secretkey", skEntry, protParam);
			} catch (KeyStoreException e) {e.printStackTrace();}
			
	        java.io.FileOutputStream file2 = null;
	        try {
	        	file2 = new java.io.FileOutputStream("../keys/client/client.jks");
				keystore.store(file2, passwordFile);
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
		//Encoder enc = Base64.getEncoder();
		//System.out.println("\n\n\n"+enc.encodeToString(symmetricKey.getEncoded())+"\n\n\n");        
        
		try{
			Enumeration<String> enumeration =keystore.aliases();
			
			while(enumeration.hasMoreElements()) {
	            String alias = (String)enumeration.nextElement();
	            System.out.println("alias: " + alias);
	            

	        }
		}catch (Exception e){System.out.println("erro");}
		
		try {
			KeyStore.PrivateKeyEntry pkEntry=null;
			try {
				pkEntry = (KeyStore.PrivateKeyEntry)keystore.getEntry("client", protParam);
			} catch (UnrecoverableEntryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    privateKey =   pkEntry.getPrivateKey();
		    publicKey = pkEntry.getCertificate().getPublicKey();
		} catch (KeyStoreException e1) {e1.printStackTrace();
		} catch (NoSuchAlgorithmException e) { e.printStackTrace();}
		
		

    }

    @AfterClass
    public static void oneTimeTearDown() {

    }


    // members


    // initialization and clean-up for each test

    @Before
    public void setUp() {
    	client = new DpmClient("http://localhost:8080/ws.API/endpoint");
    }

    @After
    public void tearDown() {
    	client = null;
    }


    // tests

    @Test
    public void CorrectExecution() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),"client", "secretkey", "1nsecure".toCharArray());
    	
    		
    	assertNotNull(client.privateKey);
    	assertNotNull(client.publicKey);
    	assertNotNull(client.symmetricKey);
    	
    	assertArrayEquals(privateKey.getEncoded(), client.privateKey.getEncoded());
    	assertArrayEquals(publicKey.getEncoded(), client.publicKey.getEncoded());
    	assertArrayEquals(symmetricKey.getEncoded(), client.symmetricKey.getEncoded());

    }
    
    @Test(expected = AlreadyInitializedException.class)
    public void doubleRegister() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),"client", "secretkey", "1nsecure".toCharArray());
    	client.init(keystore, "ins3cur3".toCharArray(),"client", "secretkey", "1nsecure".toCharArray());
    }
    
    
    
    @Test(expected = NullKeystoreElementException.class)
    public void NullPassword1() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, null,"client", "secretkey", "1nsecure".toCharArray());    	
    }
    
    @Test(expected = WrongPasswordException.class)
    public void wrongPassword2() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),"client", "secretkey", "wrong".toCharArray());  	
    }
    
    @Test(expected = NullKeystoreElementException.class)
    public void NullPassword2() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),"client", "secretkey", null);  	
    }
 
    @Test(expected = GivenAliasNotFoundException.class)
    public void WrongKeyNames1() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),"wrongclient", "secretkey", "1nsecure".toCharArray());
    }
    
    @Test(expected = NullKeystoreElementException.class)
    public void NullKeyNames1() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),null, "secretkey", "1nsecure".toCharArray());
    }
    
    @Test(expected = GivenAliasNotFoundException.class)
    public void WrongKeyNames2() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),"client", "wrongsecretKey", "1nsecure".toCharArray());
    }
    
    @Test(expected = NullKeystoreElementException.class)
    public void NullKeyNames2() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(keystore, "ins3cur3".toCharArray(),"client", null, "1nsecure".toCharArray());
    }
    
    @Test(expected = NullKeystoreElementException.class)
    public void NullKeystore() throws AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.init(null, "ins3cur3".toCharArray(),"client", "wrongsecretKey", "1nsecure".toCharArray());
    }
    
    
    
}
