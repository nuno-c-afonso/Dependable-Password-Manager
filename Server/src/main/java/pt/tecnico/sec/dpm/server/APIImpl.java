package pt.tecnico.sec.dpm.server;

import java.security.Key;

import javax.jws.WebService;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.API")
public class APIImpl implements API {

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
