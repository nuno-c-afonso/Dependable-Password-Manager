package pt.tecnico.sec.dpm.server;

import org.junit.*;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import pt.tecnico.sec.dpm.server.exceptions.NullArgException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInvalidSizeException;
import pt.tecnico.sec.dpm.server.db.*;

import static org.junit.Assert.*;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */

@RunWith(JMockit.class)
public class RegisterTest {

    // static members
	private static byte[] publicKey;
	private static byte[] exactSizeKey;
	private static byte[] biggerSizeKey;
	
	private static APIImpl APIImplTest;
	
	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	
	//Connection to the database variable
	private static Connection conn = null;
	
	//Database information
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm?useLegacyDatetimeCode=false&serverTimezone=UTC";
	private static final String USER = "root";
	private static final String PASS = "secroot2017";
	


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() throws NoSuchAlgorithmException {
    	new MockUp<APIImpl> () {
    		@Mock
    		void setMessageContext() { }
    	};
    	
    	try {
			APIImplTest = new APIImpl("http://localhost:8080/ws.api/endpoint", "ins3cur3".toCharArray(), "1nsecure".toCharArray());
		} catch (NullArgException e) {
			// It will not happen
			e.printStackTrace();
		}
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	keyGen.initialize(2048);
    	publicKey = keyGen.genKeyPair().getPublic().getEncoded();
    	keyGen.initialize(4096);
    	exactSizeKey = keyGen.genKeyPair().getPublic().getEncoded();
    	biggerSizeKey = new byte[8192];
    	Arrays.fill(biggerSizeKey, (byte) 1);
    	
    	try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Open connection
		try {
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @AfterClass
    public static void oneTimeTearDown() {
    	try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    // members

/*
    // initialization and clean-up for each test

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    
    // tests
    //Verifies if the the Register function is working correctly
    @Test
    public void correctRegister() throws PublicKeyInUseException, NullArgException, PublicKeyInvalidSizeException {
    	//call function to register
		APIImplTest.register(publicKey);
		
		String queryGetPubKey = "SELECT publickey "
	              + "FROM users "
	              + "WHERE publickey = ? ";
		
		//Get User ID
		PreparedStatement p;
		ResultSet rs = null;
	  	byte[] actualPubKey = null;
	  	
	  	// Get password
		try {
			p = conn.prepareStatement(queryGetPubKey);
			p.setBytes(1, publicKey);
			rs = p.executeQuery();
			if(rs.next())
				actualPubKey = rs.getBytes("publickey");	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertArrayEquals(actualPubKey, publicKey);
    }
    
    //Different sizes Key
    @Test
    public void exactSizeKey() throws PublicKeyInUseException, NullArgException, PublicKeyInvalidSizeException {
		APIImplTest.register(exactSizeKey);
		
		String queryGetPubKey = "SELECT publickey "
	              + "FROM users "
	              + "WHERE publickey = ? ";
		
		//Get User ID
		PreparedStatement p;
		ResultSet rs = null;
	  	byte[] actualPubKey = null;
	  	
	  	// Get password
		try {
			p = conn.prepareStatement(queryGetPubKey);
			p.setBytes(1, exactSizeKey);
			rs = p.executeQuery();
			if(rs.next())
				actualPubKey = rs.getBytes("publickey");	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		assertArrayEquals(exactSizeKey, actualPubKey);
    }
    
    @Test (expected = PublicKeyInvalidSizeException.class)
    public void biggerSizeKey() throws PublicKeyInUseException, NullArgException, PublicKeyInvalidSizeException {
		APIImplTest.register(biggerSizeKey);
    }
    
    @Test (expected = NullArgException.class)
    public void nullPublicKey() throws PublicKeyInUseException, NullArgException, PublicKeyInvalidSizeException {
    	APIImplTest.register(null);
    }
    
    @Test(expected = PublicKeyInUseException.class)
    public void registerTwicePublicKey() throws PublicKeyInUseException, NullArgException, NoSuchAlgorithmException, 
    PublicKeyInvalidSizeException {
    	//Try to register Same user twice
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	keyGen.initialize(2048);
    	byte[]pubKey = keyGen.genKeyPair().getPublic().getEncoded();
		APIImplTest.register(pubKey);
    	APIImplTest.register(pubKey);    	
    }
    */
}
