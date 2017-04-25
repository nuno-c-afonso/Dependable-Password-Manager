package pt.tecnico.sec.dpm.server.register;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

public class BonrrConnection implements ByzantineRegisterConnection {
	private PrivateKey privKey;
	private PublicKey pubKey;
	private ByzantineRegister port = null;
	
	
	public BonrrConnection(PrivateKey privKey, PublicKey pubKey, String Url) {
		this.privKey = privKey;
		this.pubKey = pubKey;
		
		// TODO: Uncomment this when there is a server running!!!
		//BonrrServerService service = new BonrrServerService();
		//port = service.getBonrrServerPort();		
	}
	
	@Override
	public List<Object> write() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> read() {
		// TODO Auto-generated method stub
		return null;
	}
}
