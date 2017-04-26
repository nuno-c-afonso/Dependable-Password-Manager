package pt.tecnico.sec.dpm.server.register;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;

import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;

public class BonrrConnection implements ByzantineRegisterConnection {
	private PrivateKey privKey;
	private PublicKey pubKey;
	private ByzantineRegister port = null;
	private String url;
	private int serverCounter = 0;
	
	
	public BonrrConnection(PrivateKey privKey, PublicKey pubKey, String Url) {
		this.privKey = privKey;
		this.pubKey = pubKey;
		this.url = Url;
		
		
		// TODO: Uncomment this when there is a server running!!!
		//BonrrServerService service = new BonrrServerService();
		//port = service.getBonrrServerPort();		
	}


	@Override
	public List<Object> write(int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, int wTS,
			byte[] cliSig) throws KeyConversionException, SigningException {
		serverCounter ++;
		byte[] serverCounterBytes = SecurityFunctions.intToByteArray(serverCounter);
    	byte[] bonrr = "bonrr".getBytes();
    	byte[] write = "WRITE".getBytes();
    	byte[] sessionIDBytes = ("" + sessionID).getBytes();
    	byte[] cliCounterBytes = SecurityFunctions.intToByteArray(cliCounter);
    	byte[] wTSBytes = SecurityFunctions.intToByteArray(wTS);
    	
    	
    	byte[] bytesToSign = SecurityFunctions.concatByteArrays(bonrr, write, serverCounterBytes, sessionIDBytes, cliCounterBytes, wTSBytes,
    			domain, username, password, cliSig);
    	
    	byte[] signedBytes = SecurityFunctions.makeDigitalSignature(privKey, bytesToSign);
    	
    	List<Object> result = port.deliverWrite(serverCounter, sessionID, cliCounter, domain, username, password, wTS, cliSig, signedBytes);
    	
		return result;
	}


	@Override
	public List<Object> read(byte[] cliPublicKey, byte[] domain, byte[] username, byte[] cliSig) {
		serverCounter++;
		byte[] serverCounterBytes = SecurityFunctions.intToByteArray(serverCounter);
    	byte[] bonrr = "bonrr".getBytes();
    	byte[] write = "WRITE".getBytes();
    	
    	byte[] bytesToSign = SecurityFunctions.concatByteArrays(bonrr, write, serverCounterBytes, cliPublicKey, domain, username, cliSig);
		
		byte[] serverSig = null;
		
		List<Object> result = port.deliverRead(serverCounter, cliPublicKey, domain, username, serverSig);
		
		return result;
	}

}
