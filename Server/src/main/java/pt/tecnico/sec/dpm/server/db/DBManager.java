package pt.tecnico.sec.dpm.server.db;

import java.sql.*;

import pt.tecnico.sec.dpm.server.exceptions.*;

public class DBManager {
	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	
	private Connection conn = null;
	private ResultSet res = null;
	
	public DBManager(String url, String username, String password) {
		// Register driver
		try {
			Class.forName(JDBC_DRIVER);
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
	
	
	// TODO: Throw the exception instead, but create a package for all the exceptions on the server side!!!
	// TODO: Be careful with SQLi!!!
	
	
	// To make a DB select query
	public ResultSet select(String q) throws ConnectionClosedException {
		if(conn == null)
			throw new ConnectionClosedException();
		
		try {
			if(res != null) {
				res.close();
				res = null;
			}
			
			Statement stmt = conn.createStatement();
			res = stmt.executeQuery(q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
		
	// To insert or update data into the tables
	public void update(String q) throws ConnectionClosedException {		
		if(conn == null)
			throw new ConnectionClosedException();
		
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// To free all resources
	public void close() {
		try {
			if(conn != null)
				conn.close();
			if(res != null)
				res.close();
			
			conn = null;
			res = null;
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}