package pt.tecnico.sec.dpm.server;

import java.sql.*;

import javax.jws.WebService;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.API")
public class APIImpl implements API {
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	private static final String DB_URL = "jdbc:mysql://localhost/";
	private static final String USER = "sec_dpm";
	private static final String PASS = USER;
	
	private Connection conn = null;
	
	// Methods to check and prepare the database
	public APIImpl() {
		try {
			// Register driver
			Class.forName(JDBC_DRIVER);
			
			// Open connection
			conn = DriverManager.getConnection(DB_URL, USER, PASS);
		} catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void register(byte[] publicKey) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void put(byte[] publicKey, byte[] domain, byte[] username, byte[] password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte[] get(byte[] publicKey, byte[] domain, byte[] username) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	// TODO: Throw the exception instead, but create a package for all the exceptions on the server side!!!
	
	
	// To make a DB query
	private ResultSet query(String q) {
		Statement stmt = null;
		ResultSet res = null;
		try {
			stmt = conn.createStatement();
			res = stmt.executeQuery(q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}
}
