package pt.tecnico.sec.dpm.server.db;

import java.sql.*;

public class DBManager {
	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	
	private Connection conn = null;
	
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
	
	
	// To make a DB select query
	public ResultSet select(String q) {
		ResultSet res = null;
		try {
			Statement stmt = conn.createStatement();
			res = stmt.executeQuery(q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
		
	// To insert data into the tables
	public void insert(String q) {		
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(q);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// To update table records
	public void update(String q) {		
		insert(q);
	}
}