package pt.tecnico.sec.dpm.broadcastClient.register;

import java.util.List;

public class BonarWriter extends BonrrWriter {
	public BonarWriter(String[] urls, int numberOfFaults) {
		super(urls, numberOfFaults);
	}
	
	@Override
	public byte[] get(byte[] domain, byte[] username) throws Exception {
		List<Object> result = super.protGet(domain, username);
		byte[] password = (byte[]) result.get(1);
		
		super.put(domain, username, password, (int) result.get(2));
		return password;
	}
}
