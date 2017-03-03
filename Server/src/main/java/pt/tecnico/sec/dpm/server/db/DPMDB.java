package pt.tecnico.sec.dpm.server.db;

public class DPMDB extends DBManager {
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sec_dpm";
	private static final String USER = "dpm_account";
	private static final String PASS = "FDvlalaland129&&";
	
	public DPMDB() {
		super(DB_URL, USER, PASS);
	}
}
