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

public class Writer {
	//Variables used during the execution of the algorithm
	private int ts;
	private int val;
	private byte[] signature;
	private int wts;
	private int rid; // this might be the READER ID
	private Map<String, HashMap<String,String[]>> readList;
	private int numberOfFaults;
	private List<ByzantineRegisterConnection> servers;
	
	private PrivateKey myPrivateKey = null;
    
    public Writer(String myUrl, KeyStore keyStore, List<String> serversUrl, ByzantineRegisterConnectionFactory bcf, int numberOfFaults) { //, PublicKey publicKey) {
        //Constructor works as the init of the algorithm
    	wts = 0;
    	readList = new HashMap<String, HashMap<String, String[]>>();
    	
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
    			cont  = (ackList.size() <= ( + numberOfFaults) / 2);
    		}
    	}
    	return;
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
			List<Object> res = brc.write(sessionID, cliCounter, domain, username, password, wTS,cliSig);
			
			//Add the ack to the acklist
	    	synchronized (ackList) {
	    		ackList.add(res);
	    	}
		}
    	
    }

    /*
     * Read method that will start the execution of the algorithm for the read request
     */
    private void read(PublicKey publicKey, byte[] domain, byte[] username) { 
    	//FIXME: Verify the signature of the client
    	rid += 1;
    	readList = new HashMap<String, HashMap<String, String[]>>();
    	//Now for all servers send a read request, this must be done in different threads  
    	for(ByzantineRegisterConnection brc : servers){
    		Thread aux = new Thread(new SendRead(brc, publicKey.getEncoded(), domain, username)); //Send object of bonrr connection, e wts
    	    aux.start();
    	}
    }
    
    /*
     * Class that is going to be execute on the thread to do the requests to the server
     */
    private class SendRead implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private byte[] cliPublicKey;
    	private byte[] domain;
    	private byte[] username;
    	
    	//FIXME: Verify this parameters
		public SendRead(ByzantineRegisterConnection brc, byte[] cliPublicKey, byte[] domain, byte[] username) {
			super();
			this.brc = brc;
			this.cliPublicKey = cliPublicKey;
			this.domain = domain;
			this.username = username;
			
		}

		@Override
		public void run() {
			//Execute the request	
			//FIXME: Pass the readList object 
			List<Object> res = brc.read(cliPublicKey, domain, username);
			
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
    
    
    
    private byte[] doSignature(byte[] publicKey, byte[] domain, byte[] username, byte[] password) {
    	//Variable need for the signature
    	final String BONRR = "bonrr";
    	byte[] bonrr = null;
    	
    	byte[] write = null;
    	final String WRITE = "WRITE";
    	
    	byte[] myUrl = null; //Get own URL
    	byte[]wtsBytes = null;
    	byte[] plainTextBytes = null;
    	
    	//Create the value byte array
    	byte[] value = new byte[publicKey.length + domain.length + username.length + password.length];
    	System.arraycopy(publicKey, 0, value, 0, publicKey.length);
    	System.arraycopy(domain, 0, value, publicKey.length, domain.length); //TODO: Confirm and change the second if it works as shown below
    	System.arraycopy(username, 0, value, (publicKey.length + domain.length), username.length);
    	System.arraycopy(password, 0, value, (publicKey.length + domain.length + username.length), password.length);
    	
    	//Parsing the text to a byte array
    	try{
        	bonrr = parseBase64Binary(BONRR);
        	write = parseBase64Binary(WRITE);
        	wtsBytes = parseBase64Binary(""+wts);
    	}catch(Exception e){
    		System.out.println("Error converting text to byte array " + e);
    		e.printStackTrace();
    		return null;
    	}
    	
    	//Concatenating the byte array that will be sign
    	System.arraycopy(bonrr, 0, plainTextBytes, plainTextBytes.length, bonrr.length);
    	System.arraycopy(myUrl, 0, plainTextBytes, plainTextBytes.length , bonrr.length);
    	System.arraycopy(write, 0, plainTextBytes, plainTextBytes.length , write.length);
    	System.arraycopy(wtsBytes, 0, plainTextBytes, plainTextBytes.length , wtsBytes.length);
    	System.arraycopy(value, 0, plainTextBytes, plainTextBytes.length , value.length);
    	
    	
    	//Creating the signature of the register information
    	Signature sig;
    	byte[] signature = null;
		try {
			sig = Signature.getInstance("SHA256WithRSA");
			sig.initSign((PrivateKey) myPrivateKey);
	    	sig.update(plainTextBytes);
	    	signature = sig.sign();   	
		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
    	//Return of the signature
    	return signature;
    }
    
    private Boolean verifySignature(byte[] signature){
    	return true;
    	/*
    	final byte[] plainBytesText = parseBase64Binary(text);
    	final byte[] plainBytesSignedText = parseBase64Binary(signedText);
    	

        try {
        	Signature sig = Signature.getInstance("SHA256WithRSA");
        	sig.initVerify(publicKey); // This is the server public
        	sig.update(plainBytesText);
            return sig.verify(plainBytesSignedText);
            
        } catch (SignatureException se) {
        	System.out.println("Client Exception verifying certeficate"+se);
        	se.printStackTrace();
            return false;
        } catch (Exception e){ System.out.println("Exception veryfying certeficate"+e);
        System.out.println("Exception veryfying certeficate"+ e);}
        return false;
        */
        
    }    
}
