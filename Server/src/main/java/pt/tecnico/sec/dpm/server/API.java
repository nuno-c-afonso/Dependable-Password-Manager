package pt.tecnico.sec.dpm.server;

import java.security.Key;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.server.exceptions.NoPasswordException;
import pt.tecnico.sec.dpm.server.exceptions.NoPublicKeyException;
import pt.tecnico.sec.dpm.server.exceptions.NullArgException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInvalidSizeException;

@WebService
public interface API {
	void register(byte[] publicKey) throws PublicKeyInUseException, NullArgException, PublicKeyInvalidSizeException;
	void put(byte[] publicKey, byte[] domain, byte[] username, byte[] password) throws NoPublicKeyException, NullArgException;
	byte[] get(byte[] publicKey, byte[] domain, byte[] username) throws NoPasswordException, NullArgException;
}
