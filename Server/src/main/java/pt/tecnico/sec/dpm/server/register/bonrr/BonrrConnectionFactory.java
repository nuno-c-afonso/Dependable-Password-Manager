package pt.tecnico.sec.dpm.server.register.bonrr;

import java.security.PrivateKey;
import java.security.PublicKey;

import pt.tecnico.sec.dpm.server.register.ByzantineRegisterConnection;
import pt.tecnico.sec.dpm.server.register.ByzantineRegisterConnectionFactory;

public class BonrrConnectionFactory implements ByzantineRegisterConnectionFactory{

	@Override
	public ByzantineRegisterConnection createConnection(PrivateKey privKey, PublicKey pubKey, String Url) {
		return new BonrrConnection(privKey, pubKey, Url);
	}
}
