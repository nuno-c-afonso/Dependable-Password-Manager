package pt.tecnico.sec.dpm.client;

import org.junit.*;

import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.GivenAliasNotFoundException;
import pt.tecnico.sec.dpm.client.exceptions.NotInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.NullKeystoreElementException;
import pt.tecnico.sec.dpm.client.exceptions.WrongPasswordException;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
public class CloseTest {

    // static members
	public static PublicKey publicKey = null;	
	public static PrivateKey privateKey = null;
	public static SecretKey symmetricKey = null;	
	public static DpmClient client = null;
	public static KeyStore keystore = null; 

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
		try {symmetricKey =  (SecretKey) keystore.getKey("secretKey", "1nsecure".toCharArray());
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
			
	       keyGenAES.init(256);
	       symmetricKey = keyGenAES.generateKey();
	       
	       KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(symmetricKey);
	       try {keystore.setEntry("secretKey", skEntry, protParam);
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
    	client.symmetricKey = symmetricKey;
    	client.publicKey = publicKey;
    	client.privateKey = privateKey;
    }

    @After
    public void tearDown() {
    }


    // tests

    @Test
    public void CorrectExecution() throws NotInitializedException, AlreadyInitializedException, NullKeystoreElementException, GivenAliasNotFoundException, WrongPasswordException {
    	client.close();
        assertNull(client.symmetricKey);
        assertNull(client.publicKey);
        assertNull(client.privateKey);
    }
    
    @Test(expected = NotInitializedException.class)
    public void NotInitialized() throws NotInitializedException {
    	client.close();
    	client.close();
    }
    
    

}
