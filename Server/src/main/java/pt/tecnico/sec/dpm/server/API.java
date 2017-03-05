package pt.tecnico.sec.dpm.server;

import java.security.Key;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.server.exceptions.NoPasswordException;
import pt.tecnico.sec.dpm.server.exceptions.NoPublicKeyException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseException;

@WebService
public interface API {
	void register(byte[] publicKey) throws PublicKeyInUseException;
	void put(byte[] publicKey, byte[] domain, byte[] username, byte[] password) throws NoPublicKeyException;
	byte[] get(byte[] publicKey, byte[] domain, byte[] username) throws NoPasswordException;
}
