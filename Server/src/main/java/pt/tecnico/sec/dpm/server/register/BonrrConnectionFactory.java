package pt.tecnico.sec.dpm.server.register;

import java.security.PrivateKey;
import java.security.PublicKey;

public class BonrrConnectionFactory implements ByzantineRegisterConnectionFactory{

	@Override
	public ByzantineRegisterConnection createConnection(PrivateKey privKey, PublicKey pubKey, String Url) {
		return new BonrrConnection(privKey, pubKey, Url);
	}
}
