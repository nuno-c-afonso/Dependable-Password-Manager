package pt.tecnico.sec.dpm.server.register;

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
	private Map<String, String> ackList;
	private int rid;
	private Map<String, String> readList; //TODO: This have to be change to accommodate the array (ts, array of values)
	private int numberOfServers;
	private int numberOfFaults;
    
    public Bonrr() {
        //Constructor works as the init of the algorithm
    	ts = 0;
    	wts = 0;
    	ackList = new HashMap<String, String>();
    	rid = 0;
    	readList = new HashMap<String, String>();
    	
    	//Gather info about the system
    	//TODO: Maybe create a dictionary or array of the information of the servers
    	numberOfServers = 10; 
    	numberOfFaults = 3;
    	
    }
    
    /*
     * Write Method that will start the execution of the algorithm for the write request
     */
    private void write() {
    	wts +=1;
    	ackList = new HashMap<String, String>(); //Samething as before
    	signature = doSignature();
    	//Now for all Server there exists send the wts, value, and signature, this must be done in different threads
    	//When the server replies call the deliver method
    	
    	
    }
    
    
    private void deliverWrite(String serverId, String ack){
    	//ADD the ack to the acklist
    	ackList.put(serverId, ack);
    	//Verify if there is enough acks to proceed with the write
    	if(ackList.size() > ((numberOfServers + numberOfFaults) / 2)){
    		//if there is enough acks trigger the write indication
    		//Clear the ackList
    		ackList = new HashMap<String, String>();
    	}
    }
    
    
    /*
     * Read method that will start the execution of the algorithm for the read request
     */
    private void read() {
    	rid += 1;
    	readList = new HashMap<String, String>();
    	//Now for all servers send a read request, this must be done in different threads    	
    }
    
    private void deliverRead() {
    	//First verify a signature the signature
    	if(true ){ //TODO: change this to the actual verification
    		if(readList.size()> ((numberOfServers + numberOfFaults) / 2)){
    			// v = highstesval(readList) TODO: have a better understanding of this
    			readList = new HashMap<String, String>();
    			//trigger the write indication
    		}
    	}
    }
    
    
    /*
     * AUX METHODS
     */
    private byte[] doSignature() {
    	return null;
    }
    
    private Boolean verifySignature(){
    	return true;
    }
    
    
    
    
}
