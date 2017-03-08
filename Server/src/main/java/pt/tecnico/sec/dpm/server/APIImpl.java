package pt.tecnico.sec.dpm.server;

import pt.tecnico.sec.dpm.server.db.*;
import pt.tecnico.sec.dpm.server.exceptions.*;

import javax.jws.HandlerChain;
import javax.jws.WebService;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.API")
@HandlerChain(file = "/handler-chain.xml")
public class APIImpl implements API {  	
	private DPMDB dbMan = null;
	
	public APIImpl() {
		dbMan = new DPMDB();
	}
	
	@Override
	public void register(byte[] publicKey) throws PublicKeyInUseException, NullArgException, PublicKeyInvalidSizeException {
		try {
			dbMan.register(publicKey);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	@Override
	public void put(byte[] publicKey, byte[] domain, byte[] username, byte[] password) throws NoPublicKeyException {
		try {
			dbMan.put(publicKey, domain, username, password);
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public byte[] get(byte[] publicKey, byte[] domain, byte[] username) throws NoPasswordException, NullArgException {
		byte[] res = null;
		
		try {
			res = dbMan.get(publicKey, domain, username);
		} catch (NoResultException nre) {
			throw new NoPasswordException();
		} catch (ConnectionClosedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// TODO: Sometimes this will return null!!!
		return res;
	}
	
	public void close() {
		dbMan.close();
	}
}
