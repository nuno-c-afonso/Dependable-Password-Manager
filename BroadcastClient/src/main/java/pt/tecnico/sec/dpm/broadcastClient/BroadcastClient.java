package pt.tecnico.sec.dpm.broadcastClient;

import java.util.ArrayList;
import java.util.List;

import pt.tecnico.sec.dpm.broadcastserver.ConnectionClosedException_Exception;
import pt.tecnico.sec.dpm.broadcastserver.KeyConversionException_Exception;
import pt.tecnico.sec.dpm.broadcastserver.NoPublicKeyException_Exception;
import pt.tecnico.sec.dpm.broadcastserver.NullArgException_Exception;
import pt.tecnico.sec.dpm.broadcastserver.SessionNotFoundException_Exception;
import pt.tecnico.sec.dpm.broadcastserver.SigningException_Exception;
import pt.tecnico.sec.dpm.broadcastserver.WrongSignatureException_Exception;


public class BroadcastClient {
	private List<BroadcastConnection> conns;
	
	public BroadcastClient(String[] urls) {

    	conns = new ArrayList<BroadcastConnection>();//TODO replace with broadcast Connection
    	for(String s : urls)
    		conns.add(new BroadcastConnection(s));
    }
	
	public void Broadcast(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig) throws ConnectionClosedException_Exception, KeyConversionException_Exception, NoPublicKeyException_Exception, NullArgException_Exception, SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception{
		for (BroadcastConnection con : conns){
			con.broadcastPut(deviceID, domain, username, password, wTs, sig);
		}
		
	}
	
}
