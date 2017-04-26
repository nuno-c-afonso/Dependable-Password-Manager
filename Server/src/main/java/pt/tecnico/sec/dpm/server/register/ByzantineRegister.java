package pt.tecnico.sec.dpm.server.register;

import java.util.List;

import javax.jws.WebService;

@WebService
public interface ByzantineRegister {
	
	// The return will have: serverCounter + 1, serverSig
	//TODO: Verify client signature
	List<Object> deliverWrite(int serverCounter, int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, int wTS, byte[] cliSig, byte[] serverSig);
	
	// The return will have: serverCounter + 1, password, wTS, serverSig
	List<Object> deliverRead(int serverCounter, byte[] cliPublicKey, byte[] domain, byte[] username, byte[] serverSig);
}
