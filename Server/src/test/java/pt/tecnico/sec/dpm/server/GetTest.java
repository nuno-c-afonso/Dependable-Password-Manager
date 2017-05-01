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
import java.security.SecureRandom;
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
	private static byte[] deviceId = new byte[32];
	private static byte[] nonce = new byte[64];
	
	private static int userId;
	private static int deviceIdInt;
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
    	
    	String queryInsertDevice = "INSERT INTO devices(deviceID, userID) "
    			                 + "VALUES (?,?)";
    	
    	String queryInsertSession = "INSERT INTO sessions (deviceID, nonce) "
    			                  + "VALUES (?, ?)";
    	
    	String queryInsertPassword = "INSERT INTO passwords (deviceID, domain, username, password, tmstamp, signature) "
				                   + "VALUES (?,?,?,?,?,?)";
    	
    	String queryGetDeviceID = "SELECT id FROM devices WHERE deviceID = ?";
    	
    	String queryGetUserId = "SELECT id "
    						  + "FROM users "
    						  + "WHERE publickey = ?";
    	
    	PreparedStatement p = null;
    	
    	SecureRandom sr = null;
		
    	try {
			sr = SecureRandom.getInstance("SHA1PRNG");
		} catch(NoSuchAlgorithmException nsae) {
			// It should not happen!
			nsae.printStackTrace();
		}
    	
    	sr.nextBytes(nonce);
    	sr.nextBytes(deviceId);
    	
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
	    	
	    	//Insert Device
	    	p = conn.prepareStatement(queryInsertDevice);
	    	p.setBytes(1, deviceId);
	    	p.setInt(2, userId);
	    	p.execute();
	    	
	    	//Get device ID
	    	p = conn.prepareStatement(queryGetDeviceID);
	    	p.setBytes(1, deviceId);
	    	p.execute();
	    	rs = p.getResultSet();
	    	rs.next();
	    	deviceIdInt = rs.getInt(1);
	    	
	    	//Insert Session
	    	p = conn.prepareStatement(queryInsertSession);
	    	p.setInt(1, deviceIdInt);
	    	p.setBytes(2, nonce);
	    	p.execute();
	    	
	    	//Insert Password
	    	p = conn.prepareStatement(queryInsertPassword);
	    	p.setInt(1, deviceIdInt);
	    	p.setBytes(2, DOMAIN);
	    	p.setBytes(3, USERNAME);
	    	p.setBytes(4, PASSWORD);
	    	p.setInt(5, 0);
	    	
	    	byte[] signature = SecurityFunctions.makeDigitalSignature(client,
	    			concatByteArrays(deviceId, DOMAIN, USERNAME, PASSWORD, "0".getBytes()));
	    		    	
	    	p.setBytes(6, signature);
	    	p.execute();	    	
	    	
	    	APIImplTest.insertSessionCounter(deviceId, nonce, 1);
	    	
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
    			concatByteArrays("get".getBytes(),  deviceId, nonce, "2".getBytes(), DOMAIN, USERNAME));
    	
    	//call function to get
    	List<Object> password = APIImplTest.get(deviceId, nonce, DOMAIN, USERNAME, sig);
    	assert(password != null);
    	assertArrayEquals((byte[]) password.get(0), PASSWORD);
    }
    
    @Test(expected = NullArgException.class)
    public void nullSignature() throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {    	
    	APIImplTest.get(deviceId, nonce, DOMAIN, USERNAME, null);
    }
    
    @Test(expected = NullArgException.class)
    public void bullArgs() throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {
    	APIImplTest.get(deviceId, nonce, null, null, "something".getBytes());
    }
    
    @Test(expected = NullArgException.class)
    public void nullDomain() throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {
    	APIImplTest.get(deviceId, nonce, null, USERNAME, "something".getBytes());    	
    }
    
    @Test(expected = NullArgException.class)
    public void nullUsername () throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException  {
    	APIImplTest.get(deviceId, nonce, DOMAIN, null, "something".getBytes());    	
    }
    
    @Test(expected = SessionNotFoundException.class)
    public void noExistingPassword () throws NoPasswordException, NullArgException, NoPublicKeyException, SigningException, SessionNotFoundException, KeyConversionException, WrongSignatureException {
    	byte[] sig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays("get".getBytes(),  "0".getBytes(), nonce, "2".getBytes(), DOMAIN, USERNAME));
    	
    	APIImplTest.get("0".getBytes(), nonce, DOMAIN, USERNAME, sig);
    }
    
    @Test(expected = WrongSignatureException.class)
    public void badlySigned () throws NoPasswordException, NullArgException, NoPublicKeyException, SigningException, SessionNotFoundException, KeyConversionException, WrongSignatureException {    	
    	APIImplTest.get(deviceId, nonce, DOMAIN, USERNAME, "fake".getBytes());
    }
}
