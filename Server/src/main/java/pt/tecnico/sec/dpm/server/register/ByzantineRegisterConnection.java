package pt.tecnico.sec.dpm.server.register;

import java.util.List;

public interface ByzantineRegisterConnection {
	public List<Object> write(int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, int wTS, byte[] cliSig);
	public List<Object> read(byte[] cliPublicKey, byte[] domain, byte[] username);
}
