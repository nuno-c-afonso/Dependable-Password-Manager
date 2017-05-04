package pt.tecnico.sec.dpm.broadcastClient;


import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.ws.WebServiceException;

import pt.tecnico.sec.dpm.broadcastserver.*;



public class BroadcastConnection {
	String url;
	BroadcastAPI port;
	
	public BroadcastConnection(String url){
		this.url = url;
		
		try {
		
			BroadcastServerService service = new BroadcastServerService(new URL(url));
			port = service.getBroadcastServerPort();
		} catch (MalformedURLException e) {
			// It will not happen!
			e.printStackTrace();
		} catch (WebServiceException e) {
				throw e;
		}
	}
	
	public  void broadcastPut(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig) throws ConnectionClosedException_Exception, KeyConversionException_Exception, NoPublicKeyException_Exception, NullArgException_Exception, SessionNotFoundException_Exception, SigningException_Exception, WrongSignatureException_Exception{
		port.broadcastPut(deviceID, domain, username, password, wTs, sig);
	}
}
