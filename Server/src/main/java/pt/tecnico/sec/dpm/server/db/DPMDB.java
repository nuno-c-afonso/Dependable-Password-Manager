package pt.tecnico.sec.dpm.server.db;

public class DPMDB extends DBManager {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/";
	private static final String USER = "sec_dpm";
	private static final String PASS = USER;
	
	public DPMDB() {
		super(DB_URL, USER, PASS);
	}
}
