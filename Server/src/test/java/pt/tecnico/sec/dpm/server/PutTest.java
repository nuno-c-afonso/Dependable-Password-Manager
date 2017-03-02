package pt.tecnico.sec.dpm.server;

import org.junit.*;

import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseExeception;

import static org.junit.Assert.*;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
public class PutTest {

    // static members
	final private byte[] PUBLICKEY = "PUBLICKEY".getBytes();
	final private byte[] USERNAME = "SECUSER".getBytes();
	final private byte[] PASSWORD = "SECPASSWORD".getBytes();
	final private byte[] DOMAIN = "SECDOMAIN.com".getBytes();
	
	private static APIImpl APIImplTest;
	//DBManager


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {
    	APIImplTest = new APIImpl();
    	//initialize DBMAnager

    }

    @AfterClass
    public static void oneTimeTearDown() {
    	//close DBManager

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
    //Verifies if the the Put function is working correctly
    @Test
    public void correctPut() {
    	//call function to register
    	APIImplTest.put(PUBLICKEY,DOMAIN, USERNAME, PASSWORD);
    	//after the return get the result from the DATABASE
    	
    	//do the assert with the expected result and the result gathered from the database
    	int queryResult = 0;
    	byte[] actualPublicKey;
    	
    	
		assertEquals(1,queryResult);
		assertEquals(PUBLICKEY, actualPublicKey);
    	
    	
        // assertEquals(expected, actual);
        // if the assert fails, the test fails
    }
}
