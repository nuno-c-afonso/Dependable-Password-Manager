package pt.tecnico.sec.dpm.client.register;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface ByzantineRegisterConnectionFactory {
	ByzantineRegisterConnection createConnection(PrivateKey privKey, PublicKey pubKey, String Url);
}
