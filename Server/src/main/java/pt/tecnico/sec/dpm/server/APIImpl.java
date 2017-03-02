package pt.tecnico.sec.dpm.server;

import pt.tecnico.sec.dpm.server.db.*;

import javax.jws.WebService;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.API")
public class APIImpl implements API {  	
	private DPMDB dbMan = null;
	
	public APIImpl() {
		dbMan = new DPMDB();
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
