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

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */

public class PutTest {

    // static members
	private static byte[] publicKey;
	private static byte[] deviceId = new byte[32];
	private static byte[] nonce = new byte[64];
	
	private static PrivateKey client = null;
	private static int userId;
	private static int deviceIdInt;
	final private byte[] USERNAME = "SECUSER".getBytes();
	final private byte[] PASSWORD = "SECPASSWORD".getBytes();
	final private byte[] DOMAIN = "SECDOMAIN.com".getBytes();
	
	//Server API
	private static APIImpl APIImplTest;
	
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
			// It will not happen
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
    public void setUp() throws NoSuchAlgorithmException, SQLException {        
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
    	
    	APIImplTest.insertSessionCounter(deviceId, nonce, 1);
    }

    @After
    public void tearDown() {
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
    
    // tests
    //Verifies if the the Put function is working correctly
    @Test
    public void correctPut() throws NoPublicKeyException, SQLException, NullArgException, SigningException, SessionNotFoundException, KeyConversionException, WrongSignatureException {
    	byte[] bdSig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays(deviceId, DOMAIN, USERNAME, PASSWORD, ("" + 0).getBytes()));
    	
    	byte[] sig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays("put".getBytes(), deviceId, nonce, ("2").getBytes(), DOMAIN, USERNAME, PASSWORD, ("" + 0).getBytes(), bdSig));
    	
    	//call Put
    	APIImplTest.put(deviceId, nonce, DOMAIN, USERNAME, PASSWORD, 0, bdSig, 2, sig);
    	
    	String queryGetPassword = "SELECT password "
	              + "FROM passwords "
	              + "WHERE deviceID = ?"
	              + " AND username = ?"
	              + " AND domain = ?"
	              + " AND tmstamp = 0"
	              + " AND signature = ?";
    	
    	
    	//Get Password
    	byte[] actualPassword = null;
    	PreparedStatement p;
		p = conn.prepareStatement(queryGetPassword);
		p.setInt(1, deviceIdInt);
    	p.setBytes(2, USERNAME);
    	p.setBytes(3, DOMAIN);
    	p.setBytes(4, bdSig);
    	ResultSet rs = p.executeQuery();
    	if(rs.next())actualPassword = rs.getBytes("password");
		assertArrayEquals(PASSWORD, actualPassword);
    }
    
    @Test
    public void updatePassword() throws NoPublicKeyException, SQLException, NullArgException, SigningException, SessionNotFoundException, KeyConversionException, WrongSignatureException {
    	PreparedStatement p;
    	final byte[] NEWPASS = "NEWPASS".getBytes();
    	byte[] actualPassword = null;
    	byte[] bdSig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays(deviceId, DOMAIN, USERNAME, PASSWORD, ("" + 0).getBytes()));
    	
    	byte[] sig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays("put".getBytes(), deviceId, nonce, ("2").getBytes(), DOMAIN, USERNAME, PASSWORD, ("" + 0).getBytes(), bdSig));
    	
    	//Insert Password
    	APIImplTest.put(deviceId, nonce, DOMAIN, USERNAME, PASSWORD, 0, bdSig, 2, sig);
    	
    	//Update Password
    	bdSig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays(deviceId, DOMAIN, USERNAME, NEWPASS, ("" + 1).getBytes()));
    	
    	sig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays("put".getBytes(), deviceId, nonce, ("4").getBytes(), DOMAIN, USERNAME, NEWPASS, ("" + 1).getBytes(), bdSig));
    	
    	APIImplTest.put(deviceId, nonce, DOMAIN, USERNAME, NEWPASS, 1, bdSig, 4, sig);
    	
    	String queryGetPassword = "SELECT password "
	              + "FROM passwords "
	              + "WHERE deviceID = ?"
	              + " AND username = ?"
	              + " AND domain = ?"
	              + " AND tmstamp = 1"
	              + " AND signature = ?";
    	
    	//Get Password
		p = conn.prepareStatement(queryGetPassword);
		p.setInt(1, deviceIdInt);
    	p.setBytes(2, USERNAME);
    	p.setBytes(3, DOMAIN);
    	p.setBytes(4, bdSig);
    	ResultSet rs = p.executeQuery();
    	if(rs.next())actualPassword = rs.getBytes("password");
		assertArrayEquals(NEWPASS, actualPassword);
    }
    
    //The session is not in the database
    @Test(expected = SessionNotFoundException.class)
    public void wrongSession() throws NoSuchAlgorithmException, NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException {
//    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
//        keyGen.initialize(2048);
//        byte[] wrongPubKey = keyGen.genKeyPair().getPublic().getEncoded();
                
        byte[] bdSig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays(deviceId, DOMAIN, USERNAME, PASSWORD, ("" + 0).getBytes()));
        
        byte[] sig = SecurityFunctions.makeDigitalSignature(client,
    			concatByteArrays("put".getBytes(), deviceId, nonce, ("2").getBytes(), DOMAIN, USERNAME, PASSWORD, ("" + 0).getBytes(), bdSig));
        
    	APIImplTest.put("0".getBytes(), nonce, DOMAIN, USERNAME, PASSWORD, 0, bdSig, 2, sig);
    }
    
    @Test(expected = NullArgException.class)
    public void nullDomain() throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException {
    	APIImplTest.put(deviceId, nonce, null, USERNAME, PASSWORD, 0, "something".getBytes(), -1, "something".getBytes());
    }
    
    @Test(expected = NullArgException.class)
    public void nullUsername() throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException {
    	APIImplTest.put(deviceId, nonce, DOMAIN, null, PASSWORD, 0, "something".getBytes(), -1, "something".getBytes());
    }
    
    @Test(expected = NullArgException.class)
    public void nullPassword() throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException {
    	APIImplTest.put(deviceId, nonce, DOMAIN, USERNAME, null, 0, "something".getBytes(), -1, "something".getBytes());
    }
}
