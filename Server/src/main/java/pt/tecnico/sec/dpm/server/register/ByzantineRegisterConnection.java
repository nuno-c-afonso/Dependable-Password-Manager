package pt.tecnico.sec.dpm.server.register;

import java.util.List;

public interface ByzantineRegisterConnection {
	
	public List<Object> write();
	public List<Object> read();

}
