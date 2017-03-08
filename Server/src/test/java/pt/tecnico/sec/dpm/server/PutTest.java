package pt.tecnico.sec.dpm.server;

import org.junit.*;

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
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
public class PutTest {

    // static members
	private static byte[] publicKey;
	private int userId;
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
	private static final String USER = "dpm_account";
	private static final String PASS = "FDvlalaland129&&";


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
    }

    // initialization and clean-up for each test

    @Before
    public void setUp() throws NoSuchAlgorithmException, SQLException {
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        publicKey = keyGen.genKeyPair().getPublic().getEncoded(); 
        

    	String queryInsertUser = "INSERT INTO users (publickey) "
    			               + "VALUES (?)";
    	String queryGetUserId = "SELECT id "
				              + "FROM users "
				              + "WHERE publickey = ? ";
    	
    	PreparedStatement p = null;
    	
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
    	System.out.println(userId);
	  
    }

    @After
    public void tearDown() {
    }

    // tests
    //Verifies if the the Put function is working correctly
    @Test
    public void correctPut() throws NoPublicKeyException, SQLException {
    	//call Put
    	APIImplTest.put(publicKey,DOMAIN, USERNAME, PASSWORD);
    	
    	String queryGetPassword = "SELECT password "
	              + "FROM passwords "
	              + "WHERE userID = ? "
	              + " AND username = ?"
	              + " AND domain = ?";
    	
    	//Get Password
    	byte[] actualPassword = null;
    	PreparedStatement p;
		p = conn.prepareStatement(queryGetPassword);
		p.setInt(1, userId);
    	p.setBytes(2, DOMAIN);
    	p.setBytes(3, USERNAME);
    	p.execute();
    	ResultSet rs = p.getResultSet();
    	if(rs.next())actualPassword = rs.getBytes("password");	
		assertArrayEquals(actualPassword, PASSWORD);
    }
    
    //The public key is not in the database
    @Test(expected = NoPublicKeyException.class)
    public void wrongPubKey() throws NoPublicKeyException, NoSuchAlgorithmException {
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        byte[] wrongPubKey = keyGen.genKeyPair().getPublic().getEncoded(); 
    	APIImplTest.put(wrongPubKey,DOMAIN, USERNAME, PASSWORD);  	
    }
    
    @Test(expected = NullArgException.class)
    public void nullPublicKey() throws NoPublicKeyException, NullArgException {
    	APIImplTest.put(null,DOMAIN, USERNAME, PASSWORD);
    }
    
    @Test(expected = NullArgException.class)
    public void nullDomain() throws NoPublicKeyException, NullArgException {
    	APIImplTest.put(publicKey, null, USERNAME, PASSWORD);
    }
    
    @Test(expected = NullArgException.class)
    public void nullUsername() throws NoPublicKeyException, NullArgException {
    	APIImplTest.put(publicKey, DOMAIN, null, PASSWORD);
    }
    
    @Test(expected = NullArgException.class)
    public void nullPassword() throws NoPublicKeyException, NullArgException {
    	APIImplTest.put(publicKey, DOMAIN, USERNAME, null);
    }
    
    
    
    
}
