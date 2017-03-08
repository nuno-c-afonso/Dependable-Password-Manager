package pt.tecnico.sec.dpm.server;

import org.junit.*;

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

/**
 *  Unit Test example
 *
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers
 */
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
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm";
	private static final String USER = "root";
	private static final String PASS = "secroot2017";
	


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() throws NoSuchAlgorithmException {
    	APIImplTest = new APIImpl();     
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	keyGen.initialize(2048);
    	publicKey = keyGen.genKeyPair().getPublic().getEncoded();
    	keyGen.initialize(4096);
    	exactSizeKey = keyGen.genKeyPair().getPublic().getEncoded();
    	keyGen.initialize(8192);
    	biggerSizeKey = keyGen.genKeyPair().getPublic().getEncoded();
    	
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


    // initialization and clean-up for each test

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    
    // tests
    //Verifies if the the Register function is working correctly
    //TODO: Check get by hand (no expect)!!!
    @Test(expected = PublicKeyInUseException.class)
    public void correctRegister() throws PublicKeyInUseException, NullArgException {
    	//call function to register
		APIImplTest.register(publicKey);
		
		String queryGetPubKey = "SELECT publickey "
	              + "FROM users "
	              + "WHERE userID = ? ";
		
		String queryGetUserId = "SELECT id "
	              + "FROM users "
	              + "WHERE publickey = ? ";
		
		//Get User ID
		PreparedStatement p;
		int userId = 0;
		ResultSet rs = null;
    	try {
			p = conn.prepareStatement(queryGetUserId);
			p.setBytes(1, publicKey);
			p.execute();
			rs = p.getResultSet();
			rs.next();
			userId = rs.getInt("id");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
  	
	  	//Get Password
	  	byte[] actualPubKey = null;
	  	
		try {
			p = conn.prepareStatement(queryGetPubKey);
			p.setInt(1, userId);
			rs = p.getResultSet();
			if(rs.next())actualPubKey = rs.getBytes("publickey");	
			assertArrayEquals(actualPubKey, publicKey);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    //Different sizes Key
    @Test(expected = PublicKeyInvalidSizeException.class )
    public void exactSizeKey() throws PublicKeyInUseException, NullArgException {
		APIImplTest.register(exactSizeKey);
    }
    
    @Test (expected = PublicKeyInvalidSizeException.class)
    public void biggerSizeKey() throws PublicKeyInUseException, NullArgException {
		APIImplTest.register(biggerSizeKey);
    }
    
    @Test (expected = NullArgException.class)
    public void nullPublicKey() throws PublicKeyInUseException, NullArgException {
    	APIImplTest.register(null);
    }
    
    @Test(expected = PublicKeyInUseException.class)
    public void registerTwicePublicKey() throws PublicKeyInUseException, NullArgException, NoSuchAlgorithmException {
    	//Try to register Same user twice
    	KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
    	keyGen.initialize(2048);
    	byte[]pubKey = keyGen.genKeyPair().getPublic().getEncoded();
		APIImplTest.register(pubKey);
    	APIImplTest.register(pubKey);    	
    }
}
