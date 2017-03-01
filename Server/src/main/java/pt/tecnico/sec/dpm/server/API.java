package pt.tecnico.sec.dpm.server;

import java.security.Key;

import javax.jws.WebService;

@WebService
public interface API {
	void register(byte[] publicKey);
	void put(byte[] publicKey, byte[] domain, byte[] username, byte[] password);
	byte[] get(byte[] publicKey, byte[] domain, byte[] username);
}
