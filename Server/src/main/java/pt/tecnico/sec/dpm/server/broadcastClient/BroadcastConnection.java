package pt.tecnico.sec.dpm.server.broadcastClient;


import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.ws.WebServiceException;

import pt.tecnico.sec.dpm.server.broadcastserver.*;

public class BroadcastConnection {
	URL url;
	BroadcastAPI port;
	
	public BroadcastConnection(URL url){
		this.url = url;
		
		try {
		
			BroadcastServerService service = new BroadcastServerService(url);
			port = service.getBroadcastServerPort();
		} catch (WebServiceException e) {
				throw e;
		}
	}
	
	public  void broadcastPut(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig) throws ConnectionClosedException_Exception, KeyConversionException_Exception, NoPublicKeyException_Exception, NullArgException_Exception, SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception{
		if(port == null) {
			try {	
				BroadcastServerService service = new BroadcastServerService(url);
				port = service.getBroadcastServerPort();
			} catch (WebServiceException e) {
					throw e;
			}
		}
		
		port.broadcastPut(deviceID, domain, username, password, wTs, sig);
	}
}
