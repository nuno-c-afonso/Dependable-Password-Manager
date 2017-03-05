package pt.tecnico.sec.dpm.server.db;

import java.sql.*;
import pt.tecnico.sec.dpm.server.exceptions.*;

public class DBManager {
	private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
	
	private Connection conn = null;
	private ResultSet res = null;
	
	protected DBManager(String url, String username, String password) {
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
	
	protected Connection getConnection() { return conn; }
	
	// TODO: Throw the exception instead, but create a package for all the exceptions on the server side!!!
	// TODO: Be careful with SQLi!!!
	
	
	// To make a DB select query
	protected ResultSet select(PreparedStatement p) throws ConnectionClosedException, NoResultException {
		if(conn == null)
			throw new ConnectionClosedException();
		
		try {
			if(res != null) {
				res.close();
				res = null;
			}
			
			if (p.execute())
				res = p.getResultSet();
			else
				throw new NoResultException();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return res;
	}
		
	// To insert or update data into the tables
	protected void update(PreparedStatement p) throws ConnectionClosedException {		
		if(conn == null)
			throw new ConnectionClosedException();
		
		try {
			p.executeUpdate();
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
			/*
			Driver d = DriverManager.getDrivers().nextElement();
			DriverManager.deregisterDriver(d);
			*/
		} catch (SQLException e) {
			e.printStackTrace();
		}
		/*
		boolean exc = false;
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while(!exc) {
			try {
				System.out.println(drivers.nextElement());
			} catch(NoSuchElementException e) {
				exc = true;
			}
		}*/
	}
}