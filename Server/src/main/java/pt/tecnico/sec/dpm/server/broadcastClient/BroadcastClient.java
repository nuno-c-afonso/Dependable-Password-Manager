package pt.tecnico.sec.dpm.server.broadcastClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.sec.dpm.server.broadcastserver.*;


public class BroadcastClient {
	private List<BroadcastConnection> conns;
	
	public BroadcastClient(String[] urls) {

    	conns = new ArrayList<BroadcastConnection>();
    	for(int i = 0; i < urls.length; i++) {
    		URL toBroadcast = null;
    		try {
    			toBroadcast = new URL(urls[i]);
    			toBroadcast = new URL(toBroadcast.getProtocol(), toBroadcast.getHost(), 20000 + i, "/ws.API/broadcast");
    		} catch (MalformedURLException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		
    		conns.add(new BroadcastConnection(toBroadcast));
    	}
    }
	
	public void Broadcast(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig){
		
		if(deviceID == null || domain == null || username == null || password == null || sig == null)
			return;
		
		for (BroadcastConnection con : conns){
			Thread thread = new Thread(){
				public void run(){
					try {
						con.broadcastPut(deviceID, domain, username, password, wTs, sig);
					} catch (ConnectionClosedException_Exception | KeyConversionException_Exception
							| NoPublicKeyException_Exception | NullArgException_Exception
							| SessionNotFoundException_Exception | SigningException_Exception
							| WrongSignatureException_Exception e) {
						e.printStackTrace();
					}
				}
			};
			thread.start();
		}
		
	}
	
	
}
