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
    }

    @After
    public void tearDown() {
    }


    // tests
    //Verifies if the the Register function is working correctly
    @Test(expected = PublicKeyInUseException.class)
    public void correctRegister() throws PublicKeyInUseException {
    	//call function to register
    	APIImplTest.register(publicKey);
    }
    
    
    @Test(expected = PublicKeyInUseException.class)
    public void registerTwicePublicKey() throws PublicKeyInUseException {
    	//Try to register Same user twice
    	APIImplTest.register(publicKey);
    	APIImplTest.register(publicKey);    	
    }
}
