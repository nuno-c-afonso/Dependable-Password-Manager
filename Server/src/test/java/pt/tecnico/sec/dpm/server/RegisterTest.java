package pt.tecnico.sec.dpm.server;

import org.junit.*;

import pt.tecnico.sec.dpm.server.exceptions.NullArgException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInvalidSizeException;
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
    public static void oneTimeSetUp() throws NoSuchAlgorithmException {
    	APIImplTest = new APIImpl();     
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	keyGen.initialize(2048);
    	publicKey = keyGen.genKeyPair().getPublic().getEncoded();
    	keyGen.initialize(4096);
    	exactSizeKey = keyGen.genKeyPair().getPublic().getEncoded();
    	keyGen.initialize(8192);
    	biggerSizeKey = keyGen.genKeyPair().getPublic().getEncoded();
    }

    @AfterClass
    public static void oneTimeTearDown() {
    }

    // members


    // initialization and clean-up for each test

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    
    // tests
    //Verifies if the the Register function is working correctly
    //TODO: Check get by hand (no expect)!!!
    @Test(expected = PublicKeyInUseException.class)
    public void correctRegister() throws PublicKeyInUseException {
    	//call function to register
		APIImplTest.register(publicKey);
    }
    
    //Different sizes Key
    @Test(expected = PublicKeyInvalidSizeException.class )
    public void exactSizeKey() throws PublicKeyInUseException {
		APIImplTest.register(exactSizeKey);
    }
    
    @Test (expected = PublicKeyInvalidSizeException.class)
    public void biggerSizeKey() throws PublicKeyInUseException {
		APIImplTest.register(biggerSizeKey);
    }
    
    @Test (expected = NullArgException.class)
    public void nullPublicKey() throws PublicKeyInUseException, NullArgException {
    	APIImplTest.register(null);
    }
    
    @Test(expected = PublicKeyInUseException.class)
    public void registerTwicePublicKey() throws PublicKeyInUseException {
    	//Try to register Same user twice
		APIImplTest.register(publicKey);
    	APIImplTest.register(publicKey);    	
    }
}
