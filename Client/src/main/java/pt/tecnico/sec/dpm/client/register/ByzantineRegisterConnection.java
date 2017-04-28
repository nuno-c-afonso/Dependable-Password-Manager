package pt.tecnico.sec.dpm.client.register;

import java.util.List;

import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;

public interface ByzantineRegisterConnection {
	public List<Object> write(int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, int wTS, byte[] cliSig) 
			throws KeyConversionException, SigningException;
	public List<Object> read(byte[] cliPublicKey, byte[] domain, byte[] username, byte[] cliSig);
}
