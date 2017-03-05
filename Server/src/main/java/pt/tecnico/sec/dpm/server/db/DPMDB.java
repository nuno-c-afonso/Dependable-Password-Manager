package pt.tecnico.sec.dpm.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.sec.dpm.server.exceptions.*;

public class DPMDB extends DBManager {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm";
	private static final String USER = "dpm_account";
	private static final String PASS = "FDvlalaland129&&";
	
	public DPMDB() {
		super(DB_URL, USER, PASS);
	}
	
	// Registers the users only when pubKey is new FIXME: BEWARE OF CONCURRENT UPDATES!!!
	public void register(byte[] pubKey) throws ConnectionClosedException, PublicKeyInUseException {
		String q = "INSERT INTO users (publickey) VALUES (?)";
		ArrayList<byte[]> lst = toArrayList(pubKey);
		
		try {			
			checkUser(lst);
			PreparedStatement p = createStatement(q, lst);
			update(p);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Checks if a user is already registered
	private void checkUser(List<byte[]> lst) throws PublicKeyInUseException, ConnectionClosedException, SQLException {
		String q = "SELECT id FROM users WHERE publickey = ?";
		PreparedStatement p = createStatement(q, lst);
		
		try {
			select(p);
		} catch (NoResultException e) {
			throw new PublicKeyInUseException();
		}
	}
	
	// Inserts/updates a password in the DB
	public void put(byte[] pubKey, byte[] domain, byte[] username, byte[] password) throws ConnectionClosedException, NoPublicKeyException {
		String getUserID = "SELECT id FROM users WHERE publickey=?";
		
		String in = "INSERT INTO passwords(userID, domain, username, password) "
				  + "VALUES ((" + getUserID + "),?, ?, ?)";
		
		String up = "UPDATE passwords "
				  + "SET password=? "
				  + "WHERE userID=(" + getUserID + ") AND domain=? AND username=?";
		
		ArrayList<byte[]> lst = toArrayList(pubKey);
		try {
			checkUser(lst);
			throw new NoPublicKeyException();
		} catch(PublicKeyInUseException pkiue) {
			// Continues execution
		} catch(SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		try {
			lst = toArrayList(password, pubKey, domain, username);
			PreparedStatement p = createStatement(up, lst);
			if(update(p) == 0) {
				lst = toArrayList(pubKey, domain, username, password);
				p = createStatement(in, lst);
				update(p);
			}
		} catch(SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// Retrieves the password from the DB
	// TODO: Must have some security checks, to prevent unauthorized access!!!
	public byte[] get(byte[] pubKey, byte[] domain, byte[] username) throws ConnectionClosedException, NoResultException {
		String q = "SELECT p.password "
				 + "FROM users AS u, passwords AS p "
				 + "WHERE u.publickey=? AND u.id = p.userID AND domain=? AND username=?";
		
		ArrayList<byte[]> lst = toArrayList(pubKey, domain, username);
		byte[] res = null;
		
		try {
			PreparedStatement p = createStatement(q, lst);
			ResultSet returned = select(p);
			res = returned.getBytes("p.password");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return res;
	}
	
	
	/***************
	 * Aux Methods *
	 ***************/
	
	// Auto-creates a prepared statement
	// TODO: Create tests for when the received arguments are null!!!
	private PreparedStatement createStatement(String q, List<byte[]> recv) throws SQLException {
		int size = recv.size();
		PreparedStatement p = getConnection().prepareStatement(q);
		
		for(int i = 1; i <= size; i++)
			p.setBytes(i, recv.get(i));
		
		return p;
	}
	
	// Creates an array list with a variable number of elements
	private ArrayList<byte[]> toArrayList(byte[]... els) {
	    ArrayList<byte[]> lst = new ArrayList<byte[]>();
		
		for (byte[] el : els)
	        lst.add(el);
		
		return lst;
	}
}
