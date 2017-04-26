package pt.tecnico.sec.dpm.server.register.bonrr;

import java.util.List;

import pt.tecnico.sec.dpm.server.register.ByzantineRegister;

public class BonrrServer implements ByzantineRegister{

	@Override
	public List<Object> deliverWrite(int serverCounter, int sessionID, int cliCounter, byte[] domain, byte[] username,
			byte[] password, int wTS, byte[] cliSig, byte[] serverSig) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> deliverRead(int serverCounter, byte[] cliPublicKey, byte[] domain, byte[] username,
			byte[] serverSig) {
		// TODO Auto-generated method stub
		return null;
	}

}
