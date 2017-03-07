package pt.tecnico.sec.dpm.server;

import org.junit.*;

import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseException;
import pt.tecnico.sec.dpm.server.db.*;

import static org.junit.Assert.*;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
public class RegisterTest {

    // static members
	private static byte[] publicKey;
	private static byte[] exactSizeKey;
	private static byte[] biggerSizeKey;
	private static APIImpl APIImplTest;
	


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {
    	APIImplTest = new APIImpl();     
    }

    @AfterClass
    public static void oneTimeTearDown() {
    }

    // members


    // initialization and clean-up for each test

    @Before
    public void setUp() throws NoSuchAlgorithmException {
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        publicKey = keyGen.genKeyPair().getPublic().getEncoded();
        keyGen.initialize(4096);
        exactSizeKey = keyGen.genKeyPair().getPublic().getEncoded();
        keyGen.initialize(8192);
        biggerSizeKey = keyGen.genKeyPair().getPublic().getEncoded();
    }

    @After
    public void tearDown() {
    }

    /*
    // tests
    //Verifies if the the Register function is working correctly
    @Test(expected = PublicKeyInUseException.class)
    public void correctRegister() {
    	//call function to register
    	try {
			APIImplTest.register(publicKey);
		} catch (PublicKeyInUseException e) {
		}
    }
    
    //Different sizes Key
    @Test//(expected = )
    public void exactSizeKey() {
    	try {
			APIImplTest.register(exactSizeKey);
		} catch (PublicKeyInUseException e) {
		}
    }
    
    @Test //(expected = )
    public void biggerSizeKey() {
    	try {
			APIImplTest.register(biggerSizeKey);
		} catch (PublicKeyInUseException e) {
		}
    }
    
    @Test(expected = PublicKeyInUseException.class)
    public void registerTwicePublicKey() {
    	//Try to register Same user twice
    	try {
			APIImplTest.register(publicKey);
	    	APIImplTest.register(publicKey);
		} catch (PublicKeyInUseException e) {
		}    	
    }*/
}
