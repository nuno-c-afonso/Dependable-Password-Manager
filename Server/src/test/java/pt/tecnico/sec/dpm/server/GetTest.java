package pt.tecnico.sec.dpm.server;

import org.junit.*;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.exceptions.*;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Base64;

/**
 *  Unit Test Get
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */

public class GetTest {

    // static members
	//User information
	private static byte[] publicKey;
	final private static byte[] DEVICE_ID = new byte[32];
	final private static byte[] NONCE = new byte[64];
	
	private static int userId;
	private static PrivateKey client = null;
	final private byte[] USERNAME = "SECUSER".getBytes();
	final private byte[] PASSWORD = "SECPASSWORD".getBytes();
	final private byte[] DOMAIN = "SECDOMAIN.com".getBytes();
	
	//Server API
	private static APIImpl APIImplTest = null;
	
	//Database Driver
	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	
	//Connection to the database variable
	private static Connection conn = null;
	
	//Database information
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm?useLegacyDatetimeCode=false&serverTimezone=UTC";
	private static final String USER = "root";
	private static final String PASS = "secroot2017";


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {
    	try {
			APIImplTest = new APIImpl("http://localhost:8080/ws.api/endpoint", "ins3cur3".toCharArray(), "1nsecure".toCharArray());
		} catch (NullArgException e) {
			// It will not happen!
			e.printStackTrace();
		}
    	
    	// Register driver
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

    // initialization and clean-up for each test
/*
    @Before
    public void setUp() throws NoSuchAlgorithmException, SigningException{
    	
    	// Generates a new user
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keys = keyGen.genKeyPair();
        publicKey = keys.getPublic().getEncoded();
        client = keys.getPrivate();    	
    	
    	String queryInsertUser = "INSERT INTO users (publickey) "
    			               + "VALUES (?)";
    	
    	String queryInsertSession = "INSERT INTO sessions (userID, nonce) "
    			                  + "VALUES (?, ?)";
    	
    	String queryInsertPassword = "INSERT INTO passwords (sessionID, domain, username, password, counter, tmstamp, signature) "
				                   + "VALUES (?,?,?,?,?,?,?)";
    	
    	String queryGetSessionId = "SELECT sessionID "
				                 + "FROM sessions "
				                 + "WHERE userID = ?";
    	
    	String queryGetUserId = "SELECT id "
    						  + "FROM users "
    						  + "WHERE publickey = ?";
    	
    	PreparedStatement p = null;
    	
    	try {
    		//Insert User
			p = conn.prepareStatement(queryInsertUser);
			p.setBytes(1, publicKey);
	    	p.execute();
	    	
	    	//Get User ID
	    	p = conn.prepareStatement(queryGetUserId);
	    	p.setBytes(1, publicKey);
	    	p.execute();
	    	ResultSet rs = p.getResultSet();
	    	rs.next();
	    	userId = rs.getInt("id");
	    	
	    	//Insert Session
	    	p = conn.prepareStatement(queryInsertSession);
	    	p.setInt(1, userId);
	    	p.setBytes(2, ("" + 0).getBytes());
	    	p.execute();
	    	
	    	//Get Session ID
	    	p = conn.prepareStatement(queryGetSessionId);
	    	p.setInt(1, userId);
	    	p.execute();
	    	rs = p.getResultSet();
	    	rs.next();
	    	sessionId = rs.getInt("sessionID");
	    	
	    	//Insert Password
	    	p = conn.prepareStatement(queryInsertPassword);
	    	p.setInt(1, sessionId);
	    	p.setBytes(2, DOMAIN);
	    	p.setBytes(3, USERNAME);
	    	p.setBytes(4, PASSWORD);
	    	p.setInt(5, 0);
	    	p.setInt(6, 0);
	    	
	    	byte[] signature = SecurityFunctions.makeDigitalSignature(client,
	    			concatByteArrays("get".getBytes(), ("" + sessionId).getBytes(), ("" + 0).getBytes(), DOMAIN, USERNAME));
	    		    	
	    	p.setBytes(7, signature);
	    	p.execute();	    	
	    	
	    	APIImplTest.insertSessionCounter(sessionId, 0);
	    	
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    //TODO: This was copied from APIImpl!!!
    private byte[] concatByteArrays(byte[]... arrays) {
		int newSize = 0;
		int counterSize = 0;
		
		for(byte[] el : arrays)
			newSize += el.length;
		
		byte[] result = new byte[newSize];
		for(byte[] el : arrays) {
			int elSize = el.length;
			System.arraycopy(el, 0, result, counterSize, elSize);
			counterSize += elSize;
		}
		
		return result;
	}

    @After
    public void tearDown() {
    	String queryRemoveUser = "DELETE FROM users WHERE id = ?";
    	try {
    		PreparedStatement p = conn.prepareStatement(queryRemoveUser);
			p.setInt(1, userId);
	    	p.execute();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    // tests
    //Verifies if the the Get function is working correctly    
    @Test
    public void correctGet() throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException {
    	byte[] sig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays("get".getBytes(), ("" + sessionId).getBytes(), ("" + 1).getBytes(), DOMAIN, USERNAME));
    	
    	//call function to get
    	List<Object> password = APIImplTest.get(sessionId, 1, DOMAIN, USERNAME, sig);
    	assert(password != null);
    	assertArrayEquals((byte[]) password.get(1), PASSWORD);
    }
    
    @Test(expected = NullArgException.class)
    public void nullSignature() throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {    	
    	APIImplTest.get(sessionId, 1, DOMAIN, USERNAME, null);
    }
    
    @Test(expected = NullArgException.class)
    public void bullArgs() throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {
    	APIImplTest.get(sessionId, 1, null, null, "something".getBytes());
    }
    
    @Test(expected = NullArgException.class)
    public void nullDomain() throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {
    	APIImplTest.get(sessionId, 1, null, USERNAME, "something".getBytes());    	
    }
    
    @Test(expected = NullArgException.class)
    public void nullUsername () throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {
    	APIImplTest.get(sessionId, 1, DOMAIN, null, "something".getBytes());    	
    }
    
    @Test(expected = SessionNotFoundException.class)
    public void noExistingPassword () throws NoPasswordException, NullArgException, NoPublicKeyException, SigningException, SessionNotFoundException, KeyConversionException, WrongSignatureException {
    	byte[] sig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays("get".getBytes(), "-1".getBytes(), ("" + 1).getBytes(), DOMAIN, USERNAME));
    	
    	APIImplTest.get(-1, 1, DOMAIN, USERNAME, sig);
    }
    
    @Test(expected = WrongSignatureException.class)
    public void badlySigned () throws NoPasswordException, NullArgException, NoPublicKeyException, SigningException, SessionNotFoundException, KeyConversionException, WrongSignatureException {    	
    	APIImplTest.get(sessionId, 1, DOMAIN, USERNAME, "fake".getBytes());
    }
*/
}
