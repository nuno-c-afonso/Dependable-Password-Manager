package pt.tecnico.sec.dpm.server.register;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;

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
    
    public Writer(String myUrl, KeyStore keyStore, List<String> serversUrl, ByzantineRegisterConnectionFactory bcf, int numberOfFaults) { //, PublicKey publicKey) {
        //Constructor works as the init of the algorithm
    	wts = 0;
    	
    	myPrivateKey = retrievePrivateKey(keyStore, "1nsecure".toCharArray(), myUrl);
    	
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
    
    private void write(int sessionID, int cliCounter, byte[] domain, byte[] username, byte[] password, byte[] cliSig) {
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
    	return; //TODO: Change this return to sen back to the client the server sig
    	
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
    private void read(PublicKey publicKey, byte[] domain, byte[] username, byte[] cliSig) { 
    	//FIXME: Verify the signature of the client
    	rid += 1; // Why is this rid important
    	//Map<>readList = new HashMap<>();
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
			
			
			//Add the new values to the readList
	    	synchronized (readList) {
	    		readList.put(serverUrl, res);
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
