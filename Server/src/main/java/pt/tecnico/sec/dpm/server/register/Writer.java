package pt.tecnico.sec.dpm.server.register;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.exceptions.*;



public class Writer {
	//Variables used during the execution of the algorithm
	private int ts;
	private int val;
	private byte[] signature;
	private int wts;
	private int rid; // this might be the READER ID
	private int numberOfFaults;
	private List<ByzantineRegisterConnection> servers;
	
	private PrivateKey myPrivateKey = null;
	private PublicKey myPublicKey = null;
    
    public Writer(String myUrl, KeyStore keyStore, List<String> serversUrl, ByzantineRegisterConnectionFactory bcf, int numberOfFaults) throws NullArgException { //, PublicKey publicKey) {
    	if(myUrl == null || keyStore == null || serversUrl == null || bcf == null)
    		throw new NullArgException();
    	 	
    	//Constructor works as the init of the algorithm
    	wts = 0;
    	
    	myPrivateKey = retrievePrivateKey(keyStore, "1nsecure".toCharArray(), myUrl);
    	myPublicKey = retrievePublicKey(keyStore, myUrl);
    	
    	//Gather info about the system 
    	this.numberOfFaults = numberOfFaults; //This should be define a priori
    	servers = new ArrayList<ByzantineRegisterConnection>();
    	for(String server : serversUrl) {
    		//Get Public key from this Server
    		PublicKey pubKey = retrievePublicKey(keyStore, server);
    		
    		ByzantineRegisterConnection brc = bcf.createConnection(myPrivateKey, pubKey, server);
    		servers.add(brc);
    		
    	}
    }
    
    /*
     * Write Method that will start the execution of the algorithm for the write request
     *
     */
    
    private void write(int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, byte[] cliSig) throws NullArgException {
    	if(domain == null || username == null || password == null || cliSig == null)
    		throw new NullArgException();
    	wts += 1; //Incrementing the write timeStamp
    	List<List<Object>> ackList = new ArrayList<List<Object>>(); //Cleaning the AckList
    	//FIXME: Verify the client signature before initiate the protocol
    	//Now for all Server send them the write information
    	for(ByzantineRegisterConnection brc : servers){
    		Thread aux = new Thread(new SendWrite(brc, wts, ackList, sessionID, cliCounter, domain, username, password, cliSig)); //Send object of bonrr connection, e wts
    	    aux.start();
    	}
    	
    	boolean cont = true;
    	while(cont) {
    		try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		synchronized(ackList) {
    			cont  = (ackList.size() <= (servers.size() + numberOfFaults) / 2);
    		}
    	}
    	return; //TODO: Change this return to send back to the client the server sig
    	
    }
    
    /*
     * Class that is going to be execute on the thread to do the requests to the server
     */
    private class SendWrite implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private int wTS;
    	private List<List<Object>> ackList;
    	private int sessionID;
    	private int cliCounter;
    	private byte[] domain;
    	private byte[] username;
    	private byte[] password;
    	private byte[] cliSig;
    	
		public SendWrite(ByzantineRegisterConnection brc, int wTS, List<List<Object>> ackList,
				int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, byte[] cliSig) { 
			this.brc = brc;
			this.wTS = wTS;
			this.ackList = ackList;
			this.sessionID = sessionID;
			this.cliCounter = cliCounter;
			this.domain = domain;
			this.username = username;
			this.password = password;
			this.cliSig = cliSig;
		}

		@Override
		public void run() {
			//Execute the request
			//int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, int wTS, byte[] cliSig
			List<Object> res;
			try {
				res = brc.write(sessionID, cliCounter, domain, username, password, wTS,cliSig);
				//Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(res);
		    	}
			} catch (KeyConversionException | SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
    	
    }

    /*
     * Read method that will start the execution of the algorithm for the read request
     * @return TODO: Change this return to return the password (and a list of the servers signatures???)
     */
    private void read(PublicKey publicKey, byte[] domain, byte[] username, byte[] cliSig) throws NullArgException { 
    	//FIXME: Verify the signature of the client
    	if(publicKey == null || domain == null || username == null || cliSig == null)
    		throw new NullArgException();
    	Map<String, List<Object>> readList = new HashMap<String, List<Object>>();
    	
    	
    	//Now for all servers send a read request, this must be done in different threads  
    	for(ByzantineRegisterConnection brc : servers){
    		Thread aux = new Thread(new SendRead(brc, publicKey.getEncoded(), domain, username, readList, cliSig)); //Send object of bonrr connection, e wts
    	    aux.start();
    	}
    	
    	boolean cont = true;
    	while(cont) {
    		try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		synchronized(readList) {
    			cont  = (readList.size() <= (servers.size() + numberOfFaults) / 2);
    		}
    	}
    	//Iterate over the readList and check which tuple has the highest timestamp and return it to the user
    	// Update the value to the one from the readList that has the biggest timestamp
    	readList = new HashMap<String, List<Object>>();
    	return; // Return the value to send back to the client
    }
    
    /*
     * Class that is going to be execute on the thread to do the requests to the server
     */
    private class SendRead implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private byte[] cliPublicKey;
    	private byte[] domain;
    	private byte[] username;
    	private byte[] cliSig;
    	private Map<String, List<Object>> readList;
    	private String serverUrl;
    	
    	//FIXME: Verify this parameters
		public SendRead(ByzantineRegisterConnection brc, byte[] cliPublicKey, byte[] domain, byte[] username, Map<String, List<Object>> readList, byte[] cliSig) {
			this.brc = brc;
			this.cliPublicKey = cliPublicKey;
			this.domain = domain;
			this.username = username;
			this.readList = readList;
			this.cliSig = cliSig;
			
		}

		@Override
		public void run() {
			//Execute the request	
			List<Object> res = brc.read(cliPublicKey, domain, username, cliSig);
			//res contains -> serverCounter + 1, password, wTS, serverSig
			
			//Verify this signature
			//byte[] bytesToSign = SecurityFunctions.concatByteArrays(bonrr, write, serverCounterBytes, sessionIDBytes, cliCounterBytes, wTSBytes,
	    	//		domain, username, password, cliSig);
			
			
			
			//FIXME: The res index might not be correct check it after the server part is done
			
			byte[] bonrr = "bonrr".getBytes();
	    	byte[] write = "WRITE".getBytes();
	    	byte[] serverCounterBytes = (byte[]) res.get(0);
	    	byte[] sessionIDBytes = ("" + res.get(1)).getBytes();
	    	byte[] cliCounterBytes = (byte[]) res.get(2);
	    	byte[] wTSBytes = (byte[])res.get(3);
	    	byte[] passwordBytes = (byte[])res.get(4);
	    	byte[] cliSigBytes = (byte[])res.get(5);
	    	byte[] wSig = (byte[])res.get(6);
	    	
	    	byte[] bytesToVerify = SecurityFunctions.concatByteArrays(bonrr, write, serverCounterBytes, sessionIDBytes, cliCounterBytes, wTSBytes,
	    			domain, username, passwordBytes, cliSigBytes);
			
	    	try {
				SecurityFunctions.checkSignature(myPublicKey, bytesToVerify, wSig);
				//Add the new values to the readList
				synchronized (readList) {
		    		readList.put(serverUrl, res); //TODO: Change this to have only the necessary information instead of having the full result
		    	}	
				
			} catch (WrongSignatureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}			
		}
    	
    }    
    
    /*
     * AUX METHODS
     */
    
    private void openKeyStore(KeyStore keystore, char[] keyStorePass, String url) {
    	FileInputStream file = null;
		
		try {
			keystore = KeyStore.getInstance("jceks");
			file = new FileInputStream("../keys/" + url + "/" + url + ".jks");
			keystore.load(file, keyStorePass);
			file.close();
		}  catch (KeyStoreException | NoSuchAlgorithmException | CertificateException
				| IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private PrivateKey retrievePrivateKey(KeyStore keystore, char[] keyPass, String url) {
		// The password is the same as the one used on the clients
		PrivateKey priv = null;
		
		try {
			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(keyPass);
			KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) keystore.getEntry(url.toLowerCase().replace('/','0'), protParam);
		    priv = pke.getPrivateKey();
		} catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return priv;
	}
    
    private PublicKey retrievePublicKey(KeyStore keystore, String serverUrl) {
    	X509Certificate cert = null;
		try {
			cert = (X509Certificate) keystore.getCertificate(serverUrl.toLowerCase().replace('/','0'));
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return cert.getPublicKey();
    }
}
