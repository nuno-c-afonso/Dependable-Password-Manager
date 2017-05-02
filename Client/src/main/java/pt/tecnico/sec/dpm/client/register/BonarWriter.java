package pt.tecnico.sec.dpm.client.register;

import java.util.List;

public class BonarWriter extends BonrrWriter {
	public BonarWriter(String[] urls, int numberOfFaults) {
		super(urls, numberOfFaults);
	}
	
	@Override
	public byte[] get(byte[] domain, byte[] username) throws Exception {
		List<Object> result = super.protGet(domain, username);
		byte[] password = (byte[]) result.get(0);
		
		super.put(domain, username, password, (int) result.get(1));
		return password;
	}
}
