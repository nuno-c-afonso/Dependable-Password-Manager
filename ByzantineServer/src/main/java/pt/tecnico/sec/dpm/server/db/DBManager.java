package pt.tecnico.sec.dpm.server.db;

import java.sql.*;
import pt.tecnico.sec.dpm.server.exceptions.*;

public class DBManager {
	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	
	private Connection conn = null;
	
	protected DBManager(String url, String username, String password) {
		// Register driver
		try {
			Class.forName(JDBC_DRIVER, false, this.getClass().getClassLoader());
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Open connection
		try {
			conn = DriverManager.getConnection(url, username, password);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected Connection getConnection() { return conn; }

	
	// To make a DB select query
	protected ResultSet select(PreparedStatement p) throws ConnectionClosedException, NoResultException {
		ResultSet res = null;
		
		if(conn == null)
			throw new ConnectionClosedException();
		
		try {			
			res = p.executeQuery();
			if(!res.next())
				throw new NoResultException();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
		
	// To insert or update data into the tables
	// Returned values: -1 (error), 0 (returned nothing), others (operation successful)
	protected int update(PreparedStatement p) throws ConnectionClosedException {		
		int status = -1;
		if(conn == null)
			throw new ConnectionClosedException();
		
		try {
			status = p.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			
			System.out.println("Duplicate entry on DB!");
		}
		
		return status;
	}
	
	// To acquire locks on the desired tables
	// NOTE: To be used only with controlled args, must NOT have user input!!!
	protected void lock(String... strs) {
		/*String q = "LOCK TABLES " + strs[0] + " " + strs[1];
		int size = strs.length;
		
		for(int i = 2; i < size; i += 2)
			q += ", " + strs[i] + " " + strs[i + 1];
		
		try {
			Statement stmt;
			stmt = conn.createStatement();
			stmt.execute(q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	// To release all locked tables
	protected void unlock() {
		/*String q = "UNLOCK TABLES";
		
		try {
			Statement stmt;
			stmt = conn.createStatement();
			stmt.execute(q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	// To free all resources
	public void close() {
		try {
			if(conn != null)
				conn.close();
			
			conn = null;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}