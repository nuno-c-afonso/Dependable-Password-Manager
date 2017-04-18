package pt.tecnico.sec.dpm.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.sec.dpm.server.exceptions.*;

public class DPMDB extends DBManager {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm?useLegacyDatetimeCode=false&serverTimezone=UTC";
	private static final String USER = "dpm_account";
	private static final String PASS = "FDvlalaland129&&";
	
	// Size is given in bytes
	private static final int MAX_KEY_SIZE = 550;
	
	public DPMDB() {
		super(DB_URL, USER, PASS);
	}
	
	// Registers the users only when pubKey is new
	public void register(byte[] pubKey) throws ConnectionClosedException, PublicKeyInUseException,
	NullArgException, PublicKeyInvalidSizeException {
		String q = "INSERT INTO users (publickey) VALUES (?)";
		
		if(pubKey == null)
			throw new NullArgException();
		
		if(pubKey.length > MAX_KEY_SIZE)
			throw new PublicKeyInvalidSizeException();
		
		ArrayList<byte[]> lst = toArrayList(pubKey);
		
		try {
			lock("users", "WRITE");
			existsUser(lst);
			unlock();
			throw new PublicKeyInUseException();
		} catch (NoPublicKeyException pkiue) {
			PreparedStatement p;
			try {
				p = createStatement(q, lst);
				update(p);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		unlock();
	}
	
	// Checks if a user is already registered
	private void existsUser(List<byte[]> lst) throws NoPublicKeyException, ConnectionClosedException, SQLException {
		String q = "SELECT id FROM users WHERE publickey = ?";
		PreparedStatement p = createStatement(q, lst);
		
		try {
			select(p);
		} catch (NoResultException e) {
			throw new NoPublicKeyException();
		}
	}
	
	// Inserts/updates a password in the DB
	public void put(byte[] pubKey, byte[] domain, byte[] username, byte[] password)
	throws ConnectionClosedException, NoPublicKeyException, NullArgException {
		String getUserID = "SELECT id FROM users WHERE publickey=?";
		
		String in = "INSERT INTO passwords(userID, domain, username, password) "
				  + "VALUES ((" + getUserID + "),?, ?, ?)";
		
		String up = "UPDATE passwords "
				  + "SET password=? "
				  + "WHERE userID=(" + getUserID + ") AND domain=? AND username=?";
		
		if(pubKey == null || domain == null || username == null || password == null)
			throw new NullArgException();
		
		ArrayList<byte[]> lst = toArrayList(pubKey);
		
		lock("users", "READ", "passwords", "WRITE");
		try {
			existsUser(lst);
		} catch(NoPublicKeyException pkiue) {
			unlock();
			throw new NoPublicKeyException();
		} catch(SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			unlock();
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
		
		unlock();
	}
	
	// Retrieves the password from the DB
	public byte[] get(byte[] pubKey, byte[] domain, byte[] username) throws ConnectionClosedException,
			NoResultException, NullArgException, NoPublicKeyException {
		
		String q = "SELECT p.password "
				 + "FROM users AS u, passwords AS p "
				 + "WHERE u.publickey=? AND u.id = p.userID AND domain=? AND username=?";
		
		if(pubKey == null || domain == null || username == null)
			throw new NullArgException();
		
		ArrayList<byte[]> lst = toArrayList(pubKey, domain, username);
		byte[] res = null;
		
		try {
			existsUser(toArrayList(pubKey));
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
	private PreparedStatement createStatement(String q, List<byte[]> recv) throws SQLException {
		int size = recv.size();
		PreparedStatement p = getConnection().prepareStatement(q);
		
		for(int i = 1; i <= size; i++)
			p.setBytes(i, recv.get(i - 1));
		
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
