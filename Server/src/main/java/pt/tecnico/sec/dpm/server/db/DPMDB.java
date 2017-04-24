package pt.tecnico.sec.dpm.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import java.util.Base64;

import pt.tecnico.sec.dpm.server.exceptions.*;

public class DPMDB extends DBManager {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm?useLegacyDatetimeCode=false&serverTimezone=UTC";
	private static final String USER = "dpm_account";
	private static final String PASS = "FDvlalaland129&&";
	
	public DPMDB() {
		super(DB_URL, USER, PASS);
	}
	
	// Registers the users only when pubKey is new
	public void register(byte[] pubKey) throws ConnectionClosedException, PublicKeyInUseException {
		String q = "INSERT INTO users (publickey) VALUES (?)";
		
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
	
	// Creates new sessions for registered users
	public int login(byte[] pubKey, byte[] nonce) throws ConnectionClosedException, NullArgException,
	NoPublicKeyException, DuplicatedNonceException {
		String q = "INSERT INTO sessions (userID, nonce) VALUES (?,?)";
		String q_rec = "SELECT sessionID FROM sessions WHERE userID = ? AND nonce = ?";
		int userid = -1;
		int sessionid = -1;
		
		if(pubKey == null || nonce == null)
			throw new NullArgException();
		
		ArrayList<byte[]> lst = toArrayList(pubKey);
		lock("users", "READ", "sessions", "WRITE");
		try {
			userid = existsUser(lst);
		} catch (NoPublicKeyException pkiue) {
			unlock();
			throw new NoPublicKeyException();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			unlock();
			return -1;
		}
		
		try {
			PreparedStatement p = getConnection().prepareStatement(q);
			p.setInt(1, userid);
			p.setBytes(2, nonce);
			if(update(p) == -1) {
				unlock();
				throw new DuplicatedNonceException();
			}
			
			try {
				p = getConnection().prepareStatement(q_rec);
				p.setInt(1, userid);
				p.setBytes(2, nonce);
				ResultSet rs = select(p);
				sessionid = rs.getInt(1);
			} catch (NoResultException e) {
				// It will not happen!
				e.printStackTrace();
			}
		} catch(SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		unlock();
		return sessionid;
	}
	
	// Checks if a user is already registered
	private int existsUser(List<byte[]> lst) throws NoPublicKeyException, ConnectionClosedException, SQLException {
		String q = "SELECT id FROM users WHERE publickey = ?";
		PreparedStatement p = createStatement(q, lst);
		int result = -1;
		
		try {
			ResultSet rs = select(p);
			result = rs.getInt(1);
		} catch (NoResultException e) {
			throw new NoPublicKeyException();
		}
		
		return result;
	}
	
	// Gets the user's public key for a specific function
	public byte[] pubKeyFromSession(int session) throws SessionNotFoundException, ConnectionClosedException{
		String q = "SELECT U.publickey FROM users as U, sessions AS S WHERE S.sessionID = ? AND S.userID = U.id";
		byte[] result = null;
		
		try {
			PreparedStatement p = getConnection().prepareStatement(q);
			p.setInt(1, session);
			ResultSet rs = select(p);
			result = rs.getBytes(1);
		} catch (NoResultException e) {
			throw new SessionNotFoundException();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return result;
	}
	
	// Inserts/updates a password in the DB
	// FIXME: Check if the records already exists && only apply the transformation iff wTS > prevWTS!!!
	public void put(int sessionID, int counter, byte[] domain, byte[] username, byte[] password, int wTS, byte[] sig)
	throws ConnectionClosedException {		
		String in = "INSERT INTO passwords(sessionID, counter, domain, username, password, tmstamp, signature) "
				  + "VALUES (?, ?, ?, ?, ?, ?, ?)";
				
		lock("passwords", "WRITE");

		try {
			PreparedStatement p = getConnection().prepareStatement(in);
			p.setInt(1, sessionID);
			p.setInt(2, counter);
			p.setBytes(3, domain);
			p.setBytes(4, username);
			p.setBytes(5, password);
			p.setInt(6, wTS);
			p.setBytes(7, sig);
			update(p);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		unlock();
	}
	
	// Retrieves the password from the DB
	public List<Object> get(byte[] pubKey, byte[] domain, byte[] username) throws ConnectionClosedException,
			NoResultException {
		
		final int RES_LEN = 4; 
		String q = "SELECT P.password, P.tmstamp, P.counter, P.signature "
				     + "FROM users AS U, sessions AS S, passwords AS P "
				     + "WHERE U.publickey = ? AND U.id = S.userID AND S.sessionID = P.sessionID "
				     	+ "AND P.domain = ? AND P.username = ? "
				     + "HAVING P.tmstamp >= ALL("
				     	+ "SELECT PW.tmstamp "
				     	+ "FROM users AS US, sessions AS SS, passwords AS PW "
				     	+ "WHERE US.publickey = ? AND US.id = SS.userID AND SS.sessionID = PW.sessionID "
				     		+ "AND PW.domain = ? AND PW.username = ?)";
		
		ArrayList<byte[]> lst = toArrayList(pubKey, domain, username, pubKey, domain, username);
		List<Object> res = null;
		
		try {
			PreparedStatement p = createStatement(q, lst);
			ResultSet returned = select(p);
			
			res = new ArrayList<Object>();
			for(int i = 0; i < RES_LEN; i++)
				res.add(returned.getObject(i + 1));
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
