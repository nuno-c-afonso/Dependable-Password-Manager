package pt.tecnico.sec.dpm.server.broadcast;

import java.net.URL;
import java.util.List;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.db.DPMDB;
import pt.tecnico.sec.dpm.server.exceptions.ConnectionClosedException;
import pt.tecnico.sec.dpm.server.exceptions.NoPublicKeyException;
import pt.tecnico.sec.dpm.server.exceptions.NullArgException;
import pt.tecnico.sec.dpm.server.exceptions.SessionNotFoundException;

@WebService(endpointInterface = "pt.tecnico.sec.dpm.server.broadcast.BroadcastAPI")
public class BroadcastServer implements BroadcastAPI {
	private DPMDB dbMan = null;
	private List<BroadcastAPI> conns = null;
	private List<String> serverUrls;
	
	public BroadcastServer(DPMDB dbMan, List<String> serverUrls) {
		this.dbMan = dbMan;
		this.serverUrls = serverUrls;
	}
	
	public void sendBroadcast(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTS, 
			byte[] bdSig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException, ConnectionClosedException {
		
		if(deviceID == null ||  domain == null || username == null || password == null || bdSig == null)
			throw new NullArgException();
		
		if(conns == null) {
			for(String url : serverUrls) {
				BroadcastServer service = null;
				
				try {
					service = new BroadcastServer(new URL(url));
				} catch (MalformedURLException e) {
					// It will not happen!
					e.printStackTrace();
				}
				
				BroadcastAPI port = service.getAPIImplPort();
				
				conns.add(port);
			}
		} else {
			for(BroadcastAPI i : conns) {
				Thread thread = new Thread(){
				    public void run(){
				    	i.broadcastPut(deviceID, domain, username, password, wTS, bdSig);
				    }
				  };
				  thread.start();
			}
		}
		
	}

	@Override
	public void broadcastPut(byte[] pubKey, byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException, ConnectionClosedException {
		boolean result = dbMan.put(pubKey, deviceID, domain, username, password, wTs, sig);
		
		if(result) {
			sendBroadcast(deviceID, domain, username, password, wTs, sig);
		}
	}
}
