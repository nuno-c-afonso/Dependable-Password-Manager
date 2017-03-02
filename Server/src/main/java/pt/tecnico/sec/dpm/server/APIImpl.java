package pt.tecnico.sec.dpm.server;

import pt.tecnico.sec.dpm.server.db.DBManager;

import javax.jws.WebService;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.API")
public class APIImpl implements API {  
	private static final String DB_URL = "jdbc:mysql://localhost/";
	private static final String USER = "sec_dpm";
	private static final String PASS = USER;
	
	private DBManager dbMan = null;
	
	// Methods to check and prepare the database
	public APIImpl() {
		dbMan = new DBManager(DB_URL, USER, PASS);
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
}
