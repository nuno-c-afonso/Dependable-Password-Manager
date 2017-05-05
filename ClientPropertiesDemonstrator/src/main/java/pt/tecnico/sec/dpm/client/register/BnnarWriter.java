package pt.tecnico.sec.dpm.client.register;

import java.util.List;

import pt.tecnico.sec.dpm.server.NoPasswordException_Exception;

public class BnnarWriter extends BonarWriter {
	public BnnarWriter(String[] urls, int numberOfFaults) {
		super(urls, numberOfFaults);
	}
	
	@Override
	public void put(byte[] domain, byte[] username, byte[] password) throws Exception {		
		try {
			List<Object> read = protGet(domain, username);
			int wTS = (int) read.get(2) + 1;
			put(domain, username, ("" + wTS).getBytes(), wTS);
		} catch(NoPasswordException_Exception e) {
			put(domain, username, "1".getBytes(), 1);
		}
    }
}
