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
	private static DBManager DB;


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {
    	APIImplTest = new APIImpl();
    	DB = new DPMDB();       
    }

    @AfterClass
    public static void oneTimeTearDown() {
    	DB.close();
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
    public void correctRegister() {
    	//call function to register
    	APIImplTest.register(publicKey);
    }
    
    
    @Test(expected = PublicKeyInUseException.class)
    public void registerTwicePublicKey() {
    	//Try to register Same user twice
    	APIImplTest.register(publicKey);
    	APIImplTest.register(publicKey);    	
    }
}
