package pt.tecnico.sec.dpm.server;

import org.junit.*;

import com.mysql.cj.api.jdbc.Statement;

import pt.tecnico.sec.dpm.server.db.DPMDB;
import pt.tecnico.sec.dpm.server.exceptions.*;

import static org.junit.Assert.*;

import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

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
	final private byte[] USERNAME = "SECUSER".getBytes();
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
    public void correctGet() throws NoPasswordException {
    	//call function to get
    	byte[] password = APIImplTest.get(publicKey, DOMAIN, USERNAME);
    	
    	byte[] res = null;
    	Statement stmt = null;
    	
    	String q = "SELECT password"
    			 + "FROM passwords"
    			 + "WHERE publicKey = " +  publicKey 
    			 + "AND domain = " + DOMAIN 
    			 + "AND username = " + USERNAME;
    	
    	try {
			stmt = (Statement) conn.createStatement();
			ResultSet rs = stmt.executeQuery(q);
			res = rs.getBytes("p.password");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	assert(password != null);
    	assert (password == res);
    }
}
