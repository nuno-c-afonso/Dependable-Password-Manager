package pt.tecnico.sec.dpm.server.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.sec.dpm.server.exceptions.*;

public class DPMDB extends DBManager {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm";
	private static final String DB_OPTIONS = "?useLegacyDatetimeCode=false&serverTimezone=UTC";
	private static final String USER = "dpm_account";
	private static final String PASS = "FDvlalaland129&&";
	
	public DPMDB() {
		super(DB_URL + DB_OPTIONS, USER, PASS);
	}
	
	public DPMDB(int index) {
		super(DB_URL + index + DB_OPTIONS, USER, PASS);
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

	// TODO: Return the last put operation made that specific user!!!
	public int login(byte[] pubKey, byte[] deviceID, byte[] nonce) throws ConnectionClosedException, NullArgException,
	NoPublicKeyException, DuplicatedNonceException {
		String q = "INSERT INTO devices (userID, deviceID) VALUES (?,?)";
		String q_rec = "SELECT id FROM devices WHERE userID = ? AND deviceID = ?";
		String q_nonce = "INSERT INTO sessions (deviceID, nonce) VALUES (?,?)";
		
		int userid;
		int id = -1;
		
		if(pubKey == null || deviceID == null || nonce == null)
			throw new NullArgException();
		
		ArrayList<byte[]> lst = toArrayList(pubKey);
		lock("users", "READ", "sessions", "WRITE", "devices", "WRITE");
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
			p.setBytes(2, deviceID);
			update(p);
			
			try {
				p = getConnection().prepareStatement(q_rec);
				p.setInt(1, userid);
				p.setBytes(2, nonce);
				ResultSet rs = select(p);
				id = rs.getInt(1);
			} catch (NoResultException e) {
				// It will not happen!
				e.printStackTrace();
			}
			
			p = getConnection().prepareStatement(q_nonce);
			p.setInt(1, id);
			p.setBytes(2, nonce);
			if(update(p) == -1)
				throw new DuplicatedNonceException();
			
		} catch(SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		unlock();
		
		// TODO: Return something usefull, like the last put!!!
		return -1;
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
	public byte[] pubKeyFromDeviceID(byte[] deviceID) throws SessionNotFoundException, ConnectionClosedException {
		String q = "SELECT U.publickey FROM users as U, devices AS D WHERE D.deviceID = ? AND S.userID = U.id";
		byte[] result = null;
		
		try {
			PreparedStatement p = getConnection().prepareStatement(q);
			p.setBytes(1, deviceID);
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
	public void put(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTS, byte[] sig)
	throws ConnectionClosedException {		
		String in = "INSERT INTO passwords(deviceID, domain, username, password, tmstamp, signature) "
				  + "VALUES (?, ?, ?, ?, ?, ?)";
				
		lock("passwords", "WRITE");

		try {
			PreparedStatement p = getConnection().prepareStatement(in);
			p.setBytes(1, deviceID);
			p.setBytes(2, domain);
			p.setBytes(3, username);
			p.setBytes(4, password);
			p.setInt(5, wTS);
			p.setBytes(6, sig);
			
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
		String q = "SELECT P.password, P.tmstamp, P.deviceID, P.signature "
				     + "FROM users AS U, devices AS D, passwords AS P "
				     + "WHERE U.publickey = ? AND U.id = D.userID AND D.deviceID = P.deviceID "
				     	+ "AND P.domain = ? AND P.username = ? "
				     + "HAVING P.tmstamp >= ALL("
				     	+ "SELECT PW.tmstamp "
				     	+ "FROM users AS US, devices AS DS, passwords AS PW "
				     	+ "WHERE US.publickey = ? AND US.id = DS.userID AND DS.deviceID = PW.deviceID "
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
