package pt.tecnico.sec.dpm.server.broadcast;

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
	
	public BroadcastServer(DPMDB dbMan) {
		this.dbMan = dbMan;
	}
	
	public void sendBroadcast(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTS, 
			byte[] bdSig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException, ConnectionClosedException {
		
		if(deviceID == null ||  domain == null || username == null || password == null || bdSig == null)
			throw new NullArgException();
		
		if(conns == null) {
			/*for(String url : serverUrls) {
				APIImplService service = null;
				
				try {
					service = new APIImplService(new URL(url));
				} catch (MalformedURLException e) {
					// It will not happen!
					e.printStackTrace();
				}
				
				BroadcastAPI port = service.getAPIImplPort();
				
				conns.add(port);
			}*/
		} else {
			for(BroadcastAPI i : conns) {
				Thread thread = new Thread(){
				    public void run(){
				    	//i.broadcastPut(deviceID, domain, username, password, wTS, bdSig);
				    }
				  };
				  thread.start();
			}
		}
		
	}

	@Override
	public void broadcastPut(byte[] deviceID, byte[] domain, byte[] username, byte[] password, int wTS, 
			byte[] bdSig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException, ConnectionClosedException {
		//put(deviceID, domain, username, password, wTS, bdSig);
		/*if(result) {
			sendBroadcast(deviceID, domain, username, password, wTS, bdSig);
		}*/
	}
}
