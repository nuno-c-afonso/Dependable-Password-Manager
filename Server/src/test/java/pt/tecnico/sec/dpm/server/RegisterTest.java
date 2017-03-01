package pt.tecnico.sec.dpm.server;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
public class RegisterTest {

    // static members
	private byte[] publicKey;


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {

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
    // Validate public key
    @Test
    public void correctRegister() {
    	//call function to register
    	//after the return get the result from the DATABASE
    	//do the assert with the expected result and the result gathered from the database
    	
    	
        // assertEquals(expected, actual);
        // if the assert fails, the test fails
    }

}
