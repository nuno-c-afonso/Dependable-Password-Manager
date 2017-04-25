package pt.tecnico.sec.dpm.server.register;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;


/*
 * Implementation of the (1,N) Byzantine Regular Register
 */

public class Bonrr {
	//Variables used during the execution of the algorithm
	private int ts;
	private int val;
	private byte[] signature;
	private int wts;
	private static Map<String, String> ackList;
	private int rid;
	private Map<String, HashMap<String,String[]>> readList;
	private int numberOfServers;
	private int numberOfFaults;
	private String[] serversInfo;
	
	private PrivateKey myPrivateKey = null;
	//private PublicKey myPublicKey = null;
    
    public Bonrr(PrivateKey privateKey, String[] serversInfo, int numberOfFaults) { //, PublicKey publicKey) {
        //Constructor works as the init of the algorithm
    	ts = 0;
    	wts = 0;
    	ackList = new HashMap<String, String>();
    	rid = 0;
    	readList = new HashMap<String, HashMap<String, String[]>>();
    	
    	//Gather info about the system 
    	this.myPrivateKey = privateKey; //Server Private Key
    	//this.myPublicKey = publicKey; // Server Public Key
    	this.serversInfo = serversInfo; //Populate the server info
    	numberOfServers = serversInfo.length;
    	this.numberOfFaults = numberOfFaults; //This should be define a priori
    	
    }
    
    /*
     * Write Method that will start the execution of the algorithm for the write request
     *
     */
    private void write(byte[] publicKey, byte[] domain, byte[] username, byte[] password) {
    	//TODO: This wts might have to be changed
    	wts += 1; //Incrementing the write timeStamp
    	ackList = new HashMap<String, String>(); //Cleaning the AckList
    	signature = doSignature(publicKey, domain, username, password); //Signature of the register information
    	//Now for all Server send them the write information
    	for(String server : serversInfo){
    		Thread aux = new Thread(new SendWrite(server));
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
    			cont  = (ackList.size() <= (numberOfServers + numberOfFaults) / 2);
    		}
    	}
    }
    /*
     * Class that is going to be execute on the thread to do the requests to the server
     */
    private class SendWrite implements Runnable {
    	String server;
    	
		public SendWrite(String server) {
			super();
			this.server = server;
		}

		@Override
		public void run() {
			//Execute the request
			
			//Add the ack to the acklist
	    	synchronized (ackList) {
	    		ackList.put(server, "ACK");
	    	}
		}
    	
    }
    
    private void ackWrite(String server, String ack){
    	//create var if i'm writing bool
    	//Verify if there is enough acks to proceed with the write
    	if(ackList.size() > ((numberOfServers + numberOfFaults) / 2)){
    		//if there is enough acks trigger the write indication
    		//TODO: Here should have a verification if a write indication have been already done done otherwise more than one indication might be done
    		//Clear the ackList
    		ackList = new HashMap<String, String>();
    	}
    }

    /*
     * Read method that will start the execution of the algorithm for the read request
     */
    private void read() {
    	rid += 1;
    	readList = new HashMap<String, HashMap<String, String[]>>();
    	//Now for all servers send a read request, this must be done in different threads  
    	for(String server : serversInfo){
    		Thread aux = new Thread(new SendRead(server));
    	    aux.start();
    	}   
    }
    
    /*
     * Class that is going to be execute on the thread to do the requests to the server
     */
    private class SendRead implements Runnable {
    	String server;
    	
		public SendRead(String server) {
			super();
			this.server = server;
		}

		@Override
		public void run() {
			//Execute the request
			
			//When receive response call deliverWrite
			byte[] signature = null;
			//the server should return a ts' and a v' and a signature
			//deliverRead(signature);
			
		}
    	
    }
    
    private void deliverRead(String server, byte[] signature, byte[] ts, byte[] value) {
    	//First verify a signature the signature
    	if(verifySignature(signature) ){
    		//readList.put(server);
    		if(readList.size()> ((numberOfServers + numberOfFaults) / 2)){
    			// v = highstesval(readList) TODO: have a better understanding of this
    			readList = new HashMap<String, HashMap<String, String[]>>();
    			//trigger the write indication
    		}
    	}
    }
    
    
    /*
     * AUX METHODS
     */
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
