package pt.tecnico.sec.dpm.client.register;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
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
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import pt.tecnico.sec.dpm.server.NoPasswordException_Exception;
import pt.tecnico.sec.dpm.server.NoPublicKeyException_Exception;
import pt.tecnico.sec.dpm.server.NullArgException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInUseException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInvalidSizeException_Exception;
import pt.tecnico.sec.dpm.server.SessionNotFoundException_Exception;
import pt.tecnico.sec.dpm.server.SigningException_Exception;
import pt.tecnico.sec.dpm.server.WrongSignatureException_Exception;


public abstract class Writer {	
	private List<ByzantineRegisterConnection> conns;
	private final float numberOfResponses;
	
	private PrivateKey privateKey = null;
	private PublicKey publicKey = null;
	private SecretKey symmetricKey = null;
	
	private int writeTS = 0;
	private byte[] deviceID = null;
	
    public Writer(String[] urls, int numberOfFaults) {
    	this.numberOfResponses = (numberOfFaults + urls.length) / 2;
    	conns = new ArrayList<ByzantineRegisterConnection>();
    	for(String s : urls)
    		conns.add(new ByzantineRegisterConnection(s));
    }
    
 	// It is assumed that all keys are protected by the same password
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
 		    deviceID = createDeviceID();
 		    
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
    
    public void register_user() throws Exception {
    	
		if(this.privateKey == null || this.publicKey == null || this.symmetricKey == null)
			throw new NotInitializedException();
		
		// Creates a new ackList to receive only the current results
		List<Integer> ackList = new ArrayList<Integer>();
		List<Exception> exceptionsList = new ArrayList<Exception>();
			
		// Makes the registration
		for(ByzantineRegisterConnection brc : conns) {
			Thread aux = new Thread(new SendRegister(brc, publicKey, ackList, exceptionsList));
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
    		
    		synchronized(exceptionsList) {
    			if(exceptionsList.size() > numberOfResponses)
    				rethrowException(exceptionsList);
    		}
    		
    		synchronized(ackList) {
    			cont  = ackList.size() <= numberOfResponses;
    		}
    	}
    	
    	// Creates a new list to receive the login results
    	ackList = new ArrayList<Integer>();
		List<Object> result = null;
		
		// Makes the log in
		for(ByzantineRegisterConnection brc : conns) {
			Thread aux = new Thread(new SendLogin(brc, publicKey, deviceID, ackList));
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
	}
    
    /*
     * Classes that are going to be executed on the thread to do the requests to the server
     */
    private class SendRegister implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private PublicKey pubKey;
    	
    	// It will store the received wTS
    	private List<Integer> ackList;
    	private List<Exception> exceptionsList;
    	
		public SendRegister(ByzantineRegisterConnection brc, PublicKey pubKey, List<Integer> ackList, List<Exception> exceptionsList) { 
			this.brc = brc;
			this.pubKey = pubKey;
			this.ackList = ackList;
			this.exceptionsList = exceptionsList;
		}

		@Override
		public void run() {
			try {
				brc.register(pubKey);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(1);
		    	}
			} catch (SigningException | WrongSignatureException | KeyConversionException_Exception |
					NullArgException_Exception | PublicKeyInvalidSizeException_Exception | SigningException_Exception
					| WrongSignatureException_Exception e) {
				
				synchronized (exceptionsList) {
		    		exceptionsList.add(e);
		    	}
			}			
		}
    }
    
    private class SendLogin implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private PublicKey pubKey;
    	private byte[] deviceID;
    	
    	// It will store the received wTS
    	private List<Integer> ackList;
    	
		public SendLogin(ByzantineRegisterConnection brc, PublicKey pubKey, byte[] deviceID, List<Integer> ackList) { 
			this.brc = brc;
			this.pubKey = pubKey;
			this.deviceID = deviceID;
			this.ackList = ackList;
		}

		@Override
		public void run() {
			try {
				brc.login(pubKey, deviceID);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(1);
		    	}
			} catch (SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
    }
    
    
    public void put(byte[] domain, byte[] username, byte[] password) throws Exception {
    	if(this.privateKey == null || this.publicKey == null || this.symmetricKey == null)
			throw new NotInitializedException();
		
		int tmpTS = writeTS + 1;
		byte[] iv = createIV(domain, username);
		byte[] cDomain = cipherWithSymmetric(symmetricKey, domain, iv);
		byte[] cUsername = cipherWithSymmetric(symmetricKey,username, iv);
		byte[] cPassword = cipherWithSymmetric(symmetricKey, password, iv);
		byte[] bdSig = null;
		
		try {
			bdSig = SecurityFunctions.makeDigitalSignature(privateKey, SecurityFunctions.concatByteArrays(deviceID, cDomain, cUsername, cPassword, ("" + tmpTS).getBytes()));
		} catch (SigningException e1) {
			// It should not happen!
			e1.printStackTrace();
		}
		
		List<Integer> ackList = new ArrayList<Integer>();
		List<Exception> exceptionsList = new ArrayList<Exception>();
		
		// Makes the put
		for(ByzantineRegisterConnection brc : conns) {
			Thread aux = new Thread(new SendPut(brc, deviceID, cDomain, cUsername, cPassword, tmpTS, bdSig, ackList, exceptionsList));
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
    		
    		synchronized(exceptionsList) {
    			if(exceptionsList.size() > numberOfResponses)
    				rethrowException(exceptionsList);
    		}
    		
    		synchronized(ackList) {
    			cont  = ackList.size() <= numberOfResponses;
    		}
    	}
		
		writeTS = tmpTS;
    }
    
    private class SendPut implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private byte[] deviceID;
    	private byte[] cDomain;
    	private byte[] cUsername;
    	private byte[] cPassword;
    	private int wTS;
    	private byte[] bdSig;
    	
    	// It will store the received wTS
    	private List<Integer> ackList;
    	private List<Exception> exceptionsList;
    	
		public SendPut(ByzantineRegisterConnection brc, byte[] deviceID, byte[] cDomain, byte[] cUsername, byte[] cPassword, int wTS, byte[] bdSig, List<Integer> ackList, List<Exception> exceptionsList) { 
			this.brc = brc;
			this.deviceID = deviceID;
			this.cDomain = cDomain;
			this.cUsername = cUsername;
			this.cPassword = cPassword;
			this.wTS = wTS;
			this.bdSig = bdSig;
			this.ackList = ackList;
			this.exceptionsList = exceptionsList;
		}
		
		@Override
		public void run() {
			
			try {
				brc.put(deviceID, cDomain, cUsername, cPassword, wTS, bdSig);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(1);
		    	}
			} catch (UnregisteredUserException | SigningException | KeyConversionException_Exception
					| NoPublicKeyException_Exception | NullArgException_Exception
					| SessionNotFoundException_Exception | SigningException_Exception
					| WrongSignatureException_Exception | WrongSignatureException e) {
				
				synchronized (exceptionsList) {
					exceptionsList.add(e);
				}
			}			
		}
    }
    
    public byte[] get(byte[] domain, byte[] username) throws Exception {
    	if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
    	
    	byte[] retrivedPassword = null;
    	
		byte[] iv = createIV(domain, username);
		byte[] cDomain = cipherWithSymmetric(symmetricKey, domain, iv);
		byte[] cUsername = cipherWithSymmetric(symmetricKey,username, iv);
		
		List<List<Object>> ackList = new ArrayList<List<Object>>();
		List<Exception> exceptionsList = new ArrayList<Exception>();
		
		// Makes the get
		for(ByzantineRegisterConnection brc : conns) {
			Thread aux = new Thread(new SendGet(brc, deviceID, cDomain, cUsername, ackList, exceptionsList));
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
    		
    		synchronized(exceptionsList) {
    			if(exceptionsList.size() > numberOfResponses)
    				rethrowException(exceptionsList);
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
		
		return retrivedPassword;
    }
    
    private class SendGet implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private byte[] deviceID;
    	private byte[] cDomain;
    	private byte[] cUsername;
    	
    	// It will store the received wTS
    	private List<List<Object>> ackList;
    	private List<Exception> exceptionsList;
    	
		public SendGet(ByzantineRegisterConnection brc, byte[] deviceID, byte[] cDomain, byte[] cUsername, List<List<Object>> ackList, List<Exception> exceptionsList) { 
			this.brc = brc;
			this.deviceID = deviceID;
			this.cDomain = cDomain;
			this.cUsername = cUsername;
			this.ackList = ackList;
			this.exceptionsList = exceptionsList;
		}

		@Override
		public void run() {
			try {
				List<Object> res = brc.get(deviceID, cDomain, cUsername);
								
				byte[] retrivedPassword = (byte[]) res.get(0);
				int wTS = (int) res.get(1);
				byte[] deviceIDWr = (byte[]) res.get(2);
				byte[] clientSig = (byte[]) res.get(3);
				
				byte[] expectedSig = SecurityFunctions.concatByteArrays(
						deviceIDWr,
						cDomain,
						cUsername,
						retrivedPassword,
						("" + wTS).getBytes());
				
				SecurityFunctions.checkSignature(publicKey, expectedSig, clientSig);
				
				res = new ArrayList<Object>();
				res.add(retrivedPassword);
				res.add(wTS);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(res);
		    	}
			}	catch (UnregisteredUserException | SigningException | KeyConversionException_Exception
						| NoPasswordException_Exception | NoPublicKeyException_Exception | NullArgException_Exception
						| SessionNotFoundException_Exception | SigningException_Exception
						| WrongSignatureException_Exception | WrongSignatureException e) {
				synchronized (exceptionsList) {
					exceptionsList.add(e);
				}
			}		    
		}
    }
    
    public void close() throws NotInitializedException {
    	if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
		
		symmetricKey = null;					
		privateKey = null;
		publicKey = null;
		writeTS = 0;
		deviceID = null;
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
    
    private byte[] createDeviceID() {		
		byte[] result = null;
		byte[] bytesKey = publicKey.getEncoded();
		byte[] bytesMAC = getDeviceMAC();
		byte[] toHash = new byte[bytesKey.length + bytesMAC.length];
		
		System.arraycopy(bytesKey, 0, toHash, 0, bytesKey.length);
		System.arraycopy(bytesMAC, 0, toHash, bytesKey.length, bytesMAC.length);
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			result = md.digest(toHash);
		} catch (NoSuchAlgorithmException e) {
			// It should not happen
			e.printStackTrace();
		}
		
		return result;
	}
    
    private byte[] getDeviceMAC() {
    	byte[] mac = null;
    	
    	try {
    		InetAddress ip = InetAddress.getLocalHost();
    		
    		Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();
    		while(eni.hasMoreElements() && mac == null)
    			mac = eni.nextElement().getHardwareAddress();
    		
    	} catch (UnknownHostException | SocketException e) {
    		e.printStackTrace();
    	}
    	
    	return mac;
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
    
    private void rethrowException(List<Exception> list) throws Exception {
    	HashMap<String, ExceptionWrapper> map = new HashMap<String, ExceptionWrapper>();
    	
    	for(Exception e : list) {
    		String name = e.getClass().getName();
    		
    		if(!map.containsKey(name))
    			map.put(name, new ExceptionWrapper(e));
    		else
    			map.get(name).incFreq();
    	}
    	
    	Iterator<Entry<String, ExceptionWrapper>> it = map.entrySet().iterator();
    	ExceptionWrapper ew = it.next().getValue();
        while (it.hasNext()) {
        	ExceptionWrapper n = it.next().getValue();
        	if(ew.getFreq() < n.getFreq())
        		ew = n;
        }
        
        ew.rethrow();
    }
    
    private class ExceptionWrapper {
    	private Exception exc;
    	private int freq;
    	
    	public ExceptionWrapper(Exception exc) {
    		this.exc = exc;
    		this.freq = 0;
    	}
    	
    	public void incFreq() { freq++; }
    	
    	public int getFreq() { return freq; }
    	
    	public String getExceptionName() { return exc.getClass().getName(); }
    	
    	public void rethrow() throws Exception { throw exc; }
    }
}
