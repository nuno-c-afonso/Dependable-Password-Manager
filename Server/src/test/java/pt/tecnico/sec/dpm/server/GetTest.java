package pt.tecnico.sec.dpm.server;

import org.junit.*;

import pt.tecnico.sec.dpm.server.db.DPMDB;
import pt.tecnico.sec.dpm.server.exceptions.*;

import static org.junit.Assert.*;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
public class GetTest {

    // static members
	private static byte[] publicKey;
	final private byte[] USERNAME = "SECUSER".getBytes();
	final private byte[] DOMAIN = "SECDOMAIN.com".getBytes();
	
	private static APIImpl APIImplTest;
	private static DPMDB DB;


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
    public void setUp() throws NoSuchAlgorithmException{
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        publicKey = keyGen.genKeyPair().getPublic().getEncoded();
    }

    @After
    public void tearDown() {
    }

    // tests
    //Verifies if the the Put function is working correctly
    @Test
    public void correctGet() {
    	//call function to get
    	APIImplTest.get(publicKey, DOMAIN, USERNAME);
    	
    	//after the return get the result from the DATABASE
    	
    	
        // assertEquals(expected, actual);
        // if the assert fails, the test fails
    }
}
