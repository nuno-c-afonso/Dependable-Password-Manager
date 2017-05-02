package pt.tecnico.sec.dpm.client.register;

public class BonarWriter extends BonrrWriter {
	public BonarWriter(String[] urls, int numberOfFaults) {
		super(urls, numberOfFaults);
	}
	
	@Override
	public byte[] get(byte[] domain, byte[] username) throws Exception {
		byte[] password = super.get(domain, username);
		super.put(domain, username, password);
		return password;
	}
}
