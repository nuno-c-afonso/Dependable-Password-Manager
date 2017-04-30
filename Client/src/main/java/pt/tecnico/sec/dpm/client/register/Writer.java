package pt.tecnico.sec.dpm.client.register;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.xml.ws.WebServiceException;

import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.ConnectionWasClosedException;
import pt.tecnico.sec.dpm.client.exceptions.GivenAliasNotFoundException;
import pt.tecnico.sec.dpm.client.exceptions.HandlerException;
import pt.tecnico.sec.dpm.client.exceptions.NotInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.NullClientArgException;
import pt.tecnico.sec.dpm.client.exceptions.NullKeystoreElementException;
import pt.tecnico.sec.dpm.client.exceptions.UnregisteredUserException;
import pt.tecnico.sec.dpm.client.exceptions.WrongNonceException;
import pt.tecnico.sec.dpm.client.exceptions.WrongPasswordException;
import pt.tecnico.sec.dpm.security.SecurityFunctions;
import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.DuplicatedNonceException_Exception;
import pt.tecnico.sec.dpm.server.KeyConversionException_Exception;
import pt.tecnico.sec.dpm.server.NoPublicKeyException_Exception;
import pt.tecnico.sec.dpm.server.NullArgException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInUseException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInvalidSizeException_Exception;
import pt.tecnico.sec.dpm.server.SigningException_Exception;
import pt.tecnico.sec.dpm.server.WrongSignatureException_Exception;


public abstract class Writer {	
	//Variables used during the execution of the algorithm
//	private int ts;
//	private int val;
//	private byte[] signature;
//	private int wts;
//	private int rid; // this might be the READER ID
//	private List<ByzantineRegisterConnection> servers;
//	
//	private PrivateKey myPrivateKey = null;
//	private PublicKey myPublicKey = null;
//    
//    public Writer(String myUrl, KeyStore keyStore, List<String> serversUrl, ByzantineRegisterConnectionFactory bcf, int numberOfFaults) { // throws NullArgException { //, PublicKey publicKey) {
//    	/*if(myUrl == null || keyStore == null || serversUrl == null || bcf == null)
//    		throw new NullArgException();*/
//    	 	
//    	//Constructor works as the init of the algorithm
//    	wts = 0;
//    	
//    	myPrivateKey = retrievePrivateKey(keyStore, "1nsecure".toCharArray(), myUrl);
//    	myPublicKey = retrievePublicKey(keyStore, myUrl);
//    	
//    	//Gather info about the system 
//    	this.numberOfFaults = numberOfFaults; //This should be define a priori
//    	servers = new ArrayList<ByzantineRegisterConnection>();
//    	for(String server : serversUrl) {
//    		//Get Public key from this Server
//    		PublicKey pubKey = retrievePublicKey(keyStore, server);
//    		
//    		ByzantineRegisterConnection brc = bcf.createConnection(myPrivateKey, pubKey, server);
//    		servers.add(brc);
//    		
//    	}
//    }
	private final static int NONCE_SIZE = 64;
	
	private List<ByzantineRegisterConnection> conns;
	private final int numberOfResponses;
	
	private PrivateKey privateKey = null;
	private PublicKey publicKey = null;
	private SecretKey symmetricKey = null;
	
	// TODO: Change this sessionID to be created in the client!!!
	private int writeTS = 0;
	
    public Writer(String[] urls, int numberOfFaults) {
    	this.numberOfResponses = (numberOfFaults + urls.length) / 2;
    	conns = new ArrayList<ByzantineRegisterConnection>();
    	for(String s : urls)
    		conns.add(new ByzantineRegisterConnection(s));
    }
    
 	// It is assumed that all keys are protected by the same password
    
    // TODO: Create the deviceID and send it to the connection!!!
    public void initConns(KeyStore keystore, char[] passwordKeystore, String cliPairName, String symmName, char[] passwordKeys)
    		throws GivenAliasNotFoundException, WrongPasswordException, AlreadyInitializedException {
    	    	
		if(this.privateKey != null && this.publicKey != null && this.symmetricKey != null)
			throw new AlreadyInitializedException();
		
    	try {
 			if(!keystore.containsAlias(cliPairName) ||  !keystore.containsAlias(symmName))
 				throw new GivenAliasNotFoundException();
 			
 			KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(passwordKeys);
 			symmetricKey = (SecretKey) keystore.getKey(symmName, passwordKeys);
 			
 			KeyStore.PrivateKeyEntry pke = (KeyStore.PrivateKeyEntry) keystore.getEntry(cliPairName, protParam);
 		    publicKey = pke.getCertificate().getPublicKey();
 		    privateKey = pke.getPrivateKey();
 			
 			for(ByzantineRegisterConnection brc : conns) {
 				String modUrl = brc.getUrl().toLowerCase().replace('/','0');
 				if(!keystore.containsAlias(modUrl))
 					throw new GivenAliasNotFoundException();
 				
 				X509Certificate cert = (X509Certificate) keystore.getCertificate(modUrl);
 				
 				brc.init(privateKey, cert);
 			}
 		    
 		} catch(UnrecoverableEntryException e) {
 			System.out.println(e.getMessage());
 			System.out.println("erro a abrir chave 1");
 			throw new WrongPasswordException();
 		} catch(NoSuchAlgorithmException | KeyStoreException e) {
 			System.out.println("erro a abrir chave 2");
 			e.printStackTrace();
 		}
    }
    
    // TODO: Check if all of these exceptions are needed!!!
    public void register_user() throws NotInitializedException, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException,
	HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception,
	WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException {
    	
		if(this.privateKey == null || this.publicKey == null || this.symmetricKey == null)
			throw new NotInitializedException();
		
		// Creates a new ackList to receive only the current results
		List<Integer> ackList = new ArrayList<Integer>();
		
		try {
			
			// Makes the registration
			for(ByzantineRegisterConnection brc : conns) {
				Thread aux = new Thread(new SendRegister(brc, publicKey, ackList));
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
	    			cont  = ackList.size() <= numberOfResponses;
	    		}
	    	}
	    				
			// TODO: This will not work! Check what to do in order to make a login operation with a fixed ssid
			// TODO: Maybe, the created nonce is the starting value for the counter!!!
			// TODO: Instead of getting random bytes, get a random integer. However, there my be some overlapping.
			// TODO: Use the nonce + a counter. This will guarantee a session for each device.
			
	    	// Creates a new list to receive the login results
	    	ackList = new ArrayList<Integer>();
			SecureRandom sr = null;
			
	    	try {
				sr = SecureRandom.getInstance("SHA1PRNG");
			} catch(NoSuchAlgorithmException nsae) {
				// It should not happen!
				nsae.printStackTrace();
			}
			
			byte[] nonce = new byte[NONCE_SIZE];
			List<Object> result = null;
			sr.nextBytes(nonce);
				
			// Makes the log in
			for(ByzantineRegisterConnection brc : conns) {
				Thread aux = new Thread(new SendLogin(brc, publicKey, nonce, ackList));
				aux.start();
			}
			
			cont = true;
	    	while(cont) {
	    		try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		
	    		synchronized(ackList) {
	    			cont  = ackList.size() <= numberOfResponses;
	    		}
	    	}
			
			writeTS = Collections.max(ackList);
			
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		} catch (WebServiceException e) {
			checkWebServiceException(e);
		}
	}
    
    /*
     * Classes that are going to be executed on the thread to do the requests to the server
     */
    private class SendRegister implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private PublicKey pubKey;
    	
    	// It will store the received wTS
    	private List<Integer> ackList;
    	
		public SendRegister(ByzantineRegisterConnection brc, PublicKey pubKey, List<Integer> ackList) { 
			this.brc = brc;
			this.pubKey = pubKey;
			this.ackList = ackList;
		}

		@Override
		public void run() {
			try {
				brc.register(pubKey);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(1);
		    	}
			} catch (KeyConversionException | SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
    }
    
    private class SendLogin implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private PublicKey pubKey;
    	private byte[] nonce;
    	
    	// It will store the received wTS
    	private List<Integer> ackList;
    	
		public SendLogin(ByzantineRegisterConnection brc, PublicKey pubKey, byte[] nonce, List<Integer> ackList) { 
			this.brc = brc;
			this.pubKey = pubKey;
			this.nonce = nonce;
			this.ackList = ackList;
		}

		@Override
		public void run() {
			try {
				int wTS = brc.login(pubKey, nonce);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(wTS);
		    	}
			} catch (KeyConversionException | SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
    }
    
    
    public void put(byte[] domain, byte[] username, byte[] password) {
    	if(this.privateKey == null || this.publicKey == null || this.symmetricKey == null)
			throw new NotInitializedException();
		
		try {
			int tmpTS = writeTS + 1;
			byte[] iv = createIV(domain, username);
			byte[] cDomain = cipherWithSymmetric(symmetricKey, domain, iv);
			byte[] cUsername = cipherWithSymmetric(symmetricKey,username, iv);
			byte[] cPassword = cipherWithSymmetric(symmetricKey, password, iv);
						
			List<Integer> ackList = new ArrayList<Integer>();
			
			// Makes the put
			for(ByzantineRegisterConnection brc : conns) {
				Thread aux = new Thread(new SendPut(brc, cDomain, cUsername, cPassword, tmpTS, ackList));
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
	    			cont  = ackList.size() <= numberOfResponses;
	    		}
	    	}
			
			writeTS = tmpTS;
		} catch (NoPublicKeyException_Exception e) {
			throw new UnregisteredUserException();
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		} catch (WebServiceException e) {
			checkWebServiceException(e);
		}
    }
    
    private class SendPut implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private byte[] cDomain;
    	private byte[] cUsername;
    	private byte[] cPassword;
    	private int wTS;
    	
    	// It will store the received wTS
    	private List<Integer> ackList;
    	
		public SendPut(ByzantineRegisterConnection brc, byte[] cDomain, byte[] cUsername, byte[] cPassword, int wTS, List<Integer> ackList) { 
			this.brc = brc;
			this.cDomain = cDomain;
			this.cUsername = cUsername;
			this.cPassword = cPassword;
			this.wTS = wTS;
			this.ackList = ackList;
		}

		@Override
		public void run() {
			try {
				brc.put(cDomain, cUsername, cPassword, wTS);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(1);
		    	}
			} catch (KeyConversionException | SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    public byte[] get(byte[] domain, byte[] username) throws NotInitializedException {
    	if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
    	
    	byte[] retrivedPassword = null;
    	
		try {
			byte[] iv = createIV(domain, username);
			byte[] cDomain = cipherWithSymmetric(symmetricKey, domain, iv);
			byte[] cUsername = cipherWithSymmetric(symmetricKey,username, iv);
			
			List<List<Object>> ackList = new ArrayList<List<Object>>();
			
			// Makes the get
			for(ByzantineRegisterConnection brc : conns) {
				Thread aux = new Thread(new SendGet(brc, cDomain, cUsername, ackList));
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
	    			cont  = ackList.size() <= numberOfResponses;
	    		}
	    	}
			
	    	// TODO: If there is a MITM, this operation may not end!!!
	    	// TODO: Try to contact the problematic response again!!!
	    	// TODO: Maybe have a global variable that say that it should continue trying!!!
	    	
	    	List<Object> newestTS = recoverNewestWrite(ackList);
			
	    	retrivedPassword = (byte[]) newestTS.get(0);
	    	retrivedPassword = decipherWithSymmetric(symmetricKey,retrivedPassword, iv);
			writeTS = (int) newestTS.get(1);
		} catch(NoPublicKeyException_Exception e) {
			throw new UnregisteredUserException();
		} catch (NullArgException_Exception e) {
			// It should not occur
			System.out.println(e.getMessage());
		} catch (WebServiceException e) {
			checkWebServiceException(e);
		}
		
		return retrivedPassword;
    }
    
    private List<Object> recoverNewestWrite(List<List<Object>> ackList) {
    	List<Object> result = ackList.get(0);
    	
    	int nAcks = ackList.size();
    	for(int i = 1; i < nAcks; i++)
    		// The second entry is the write TS
    		if((int) result.get(1) < (int) ackList.get(i).get(1))
    			result = ackList.get(i);
    		
    	return result;
    }
    
    private class SendGet implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private byte[] cDomain;
    	private byte[] cUsername;
    	
    	// It will store the received wTS
    	private List<List<Object>> ackList;
    	
		public SendGet(ByzantineRegisterConnection brc, byte[] cDomain, byte[] cUsername, List<List<Object>> ackList) { 
			this.brc = brc;
			this.cDomain = cDomain;
			this.cUsername = cUsername;
			this.ackList = ackList;
		}

		@Override
		public void run() {
			try {
				List<Object> res = brc.get(cDomain, cUsername);
				
				// TODO: Before appending, check if the write was performed by the user!!! (check the current user's write signature)
				
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(res);
		    	}
			} catch (KeyConversionException | SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
    
    // TODO: Check the instructions below!!!
    
    /*
     * Read method that will start the execution of the algorithm for the read request
     * @return TODO: Change this return to return the password (and a list of the servers signatures???)
     */
    private void read(PublicKey publicKey, byte[] domain, byte[] username, byte[] cliSig) { //throws NullArgException { 
    	//FIXME: Verify the signature of the client
    	/*if(publicKey == null || domain == null || username == null || cliSig == null)
    		throw new NullArgException();*/
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
		    		readList.put(serverUrl, res);
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
        
    public byte[] cipherWithSymmetric(SecretKey key, byte[] data, byte[] iv){
		byte[] returnData = null;
		try {
	        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        c.init(Cipher.ENCRYPT_MODE, key,new IvParameterSpec(iv));
	        returnData = c.doFinal(data);
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }

		return returnData;		
	}
	
	public byte[] decipherWithSymmetric(SecretKey key, byte[] ecryptedData,byte[] iv) {
		byte[] returnData = null;
		try {
	        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        c.init(Cipher.DECRYPT_MODE, key,new IvParameterSpec(iv) );
	        returnData = c.doFinal(ecryptedData);
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }

		return returnData;
	}
    
    private byte[] createIV(byte[] domain, byte[] username) {		
		byte[] result = new byte[16];
		byte[] bytesKey = publicKey.getEncoded();
		byte[] toHash = new byte[bytesKey.length + domain.length + username.length];
		
		System.arraycopy(bytesKey, 0, toHash, 0, bytesKey.length);
		System.arraycopy(domain, 0, toHash, bytesKey.length, domain.length);
		System.arraycopy(username, 0, toHash, bytesKey.length + domain.length, username.length);
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(toHash);
			System.arraycopy(hash, 0, result, 0, 16);
		} catch (NoSuchAlgorithmException e) {
			// It should not happen
			e.printStackTrace();
		}
		
		return result;
	}
}
