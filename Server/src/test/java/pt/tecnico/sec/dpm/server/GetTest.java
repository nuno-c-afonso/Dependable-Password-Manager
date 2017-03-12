package pt.tecnico.sec.dpm.server;

import org.junit.*;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;
import pt.tecnico.sec.dpm.server.exceptions.*;

import static org.junit.Assert.*;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *  Unit Test Get
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */

@RunWith(JMockit.class)
public class GetTest {

    // static members
	//User information
	private static byte[] publicKey;
	private static int userId;
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
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm";
	private static final String USER = "root";
	private static final String PASS = "secroot2017";


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {
    	APIImplTest = new APIImpl();
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
    public void setUp() throws NoSuchAlgorithmException{
    	new MockUp<APIImpl> () {
    		@Mock
    		void setMessageContext() { }
    	};
    	
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        publicKey = keyGen.genKeyPair().getPublic().getEncoded();

    	
    	
    	String queryInsertUser = "INSERT INTO users (publickey) "
    			               + "VALUES (?)";
    	String queryInsertPassword = "INSERT INTO passwords (userID, domain, username, password) "
				                   + "VALUES (?,?,?,?)";
    	String queryGetUserId = "SELECT id "
				              + "FROM users "
				              + "WHERE publickey = ? ";
    	
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
	    	
	    	//Insert Password
	    	p = conn.prepareStatement(queryInsertPassword);
	    	p.setInt(1, userId);
	    	p.setBytes(2, DOMAIN);
	    	p.setBytes(3, USERNAME);
	    	p.setBytes(4, PASSWORD);
	    	p.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
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
    public void correctGet() throws NoPasswordException, NullArgException, NoPublicKeyException {
    	//call function to get
    	byte[] password = APIImplTest.get(publicKey, DOMAIN, USERNAME);
    	assert(password != null);
    	assertArrayEquals(password, PASSWORD);
    }
    // create test for each field being null
    @Test(expected = NullArgException.class)
    public void nullPubKey() throws NoPasswordException, NullArgException, NoPublicKeyException  {
    	APIImplTest.get(null, DOMAIN, USERNAME);
    }
    
    @Test(expected = NullArgException.class)
    public void nullDomain() throws NoPasswordException, NullArgException, NoPublicKeyException  {
    	APIImplTest.get(publicKey, null, USERNAME);    	
    }
    
    @Test(expected = NullArgException.class)
    public void nullUsername () throws NoPasswordException, NullArgException, NoPublicKeyException  {
    	APIImplTest.get(publicKey, DOMAIN, null);    	
    }
    
    @Test(expected = NoPublicKeyException.class)
    public void noExistingPassword () throws NoPasswordException, NullArgException, NoPublicKeyException {
    	APIImplTest.get("INVALIDPUBKEY".getBytes(), "INVALIDDOMAIN".getBytes(), "INVALIDUSERNAME".getBytes());
    }
    
}
