package pt.tecnico.sec.dpm.client.bonrr;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.client.register.ByzantineRegister;

public class BonrrServer implements ByzantineRegister{
	private int counter;
	//private DPMDB dbMan;
	
	public BonrrServer() {
		counter = 0;
		//dbMan = new DPMDB();
	}
	
	// TODO: Add the URL of the sender!!!
	
	@Override
	public List<Object> deliverWrite(int serverCounter, int sessionID, int cliCounter, byte[] domain, byte[] username,
			byte[] password, int wTS, byte[] cliSig, byte[] serverSig) {
				
//		if(domain == null || username == null || password == null || cliSig == null || serverSig == null)
//			throw new NullArgException();
//		
//		int tmpCounter = counter + 1;
//		
//		//Start the Algorithm
//		// TODO: Verify signature
//		try {
//			byte[] publicKey = dbMan.pubKeyFromSession(sessionID);
//			
//			PublicKey pubKey = SecurityFunctions.byteArrayToPubKey(publicKey);
//			SecurityFunctions.checkSignature(pubKey,
//					SecurityFunctions.concatByteArrays("put".getBytes(),("" + sessionID).getBytes(), ("" + matchingCounter).getBytes(),
//							domain, username, password, ("" + wTs).getBytes()),
//					sig);
//			
//			// FIXME: Make the needed checks for when updating (byzantine algorithms)!!!
//			dbMan.put(sessionID, counter, domain, username, password, wTs, sig);
//		} catch (ConnectionClosedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//		
//		int updateCounter = matchingCounter + 1;
//		sessionCounters.put(sessionID, updateCounter);				
//		byte[] serverSig = SecurityFunctions.makeDigitalSignature(privKey, SecurityFunctions.concatByteArrays("put".getBytes(),
//				("" + sessionID).getBytes(), ("" + updateCounter).getBytes()));
//		
//		List<Object> res = new ArrayList<Object>();
//		res.add(updateCounter);
//		res.add(serverSig);
//		return res;
//		
//		// TODO: Check if the current result is older than the received one
//		// TODO: Update if it is
//		
//		tmpCounter++;
//		// TODO: Return: serverCounter + 1, serverSig
//		
//		counter = tmpCounter;
		
		return null;
	}

	@Override
	public List<Object> deliverRead(int serverCounter, byte[] cliPublicKey, byte[] domain, byte[] username, byte[] serverSig) {
		// TODO Auto-generated method stub
		return null;
	}

}
