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
	private final int numberOfResponses;
	
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
    
    public void register_user() throws NotInitializedException, PublicKeyInvalidSizeException_Exception, ConnectionWasClosedException,
	HandlerException, SigningException, KeyConversionException_Exception, SigningException_Exception,
	WrongSignatureException_Exception, WrongSignatureException, NoPublicKeyException_Exception, WrongNonceException {
    	
		if(this.privateKey == null || this.publicKey == null || this.symmetricKey == null)
			throw new NotInitializedException();
		
		// Creates a new ackList to receive only the current results
		List<Integer> ackList = new ArrayList<Integer>();
			
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
			} catch (SigningException | WrongSignatureException | KeyConversionException_Exception |
					NullArgException_Exception | PublicKeyInvalidSizeException_Exception | SigningException_Exception
					| WrongSignatureException_Exception e) {
				
				// TODO Auto-generated catch block
				e.printStackTrace();
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
				int wTS = brc.login(pubKey, deviceID);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(wTS);
		    	}
			} catch (SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
    }
    
    
    public void put(byte[] domain, byte[] username, byte[] password) throws NotInitializedException {
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
		
		// Makes the put
		for(ByzantineRegisterConnection brc : conns) {
			Thread aux = new Thread(new SendPut(brc, deviceID, cDomain, cUsername, cPassword, tmpTS, bdSig, ackList));
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
    	
		public SendPut(ByzantineRegisterConnection brc, byte[] deviceID, byte[] cDomain, byte[] cUsername, byte[] cPassword, int wTS, byte[] bdSig, List<Integer> ackList) { 
			this.brc = brc;
			this.deviceID = deviceID;
			this.cDomain = cDomain;
			this.cUsername = cUsername;
			this.cPassword = cPassword;
			this.wTS = wTS;
			this.bdSig = bdSig;
			this.ackList = ackList;
		}
		
		// TODO: Check the exceptions in a better way!!!
		
		@Override
		public void run() {
			try {
				brc.put(deviceID, cDomain, cUsername, cPassword, wTS, bdSig);
				
				// Add the ack to the acklist
		    	synchronized (ackList) {
		    		ackList.add(1);
		    	}
			} catch (SigningException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoPublicKeyException_Exception e) {
				//throw new UnregisteredUserException();
				
				// TODO: Check this case!!!
				
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			} catch (NullArgException_Exception e) {
				// It should not occur
				System.out.println(e.getMessage());
			} catch (UnregisteredUserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyConversionException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SessionNotFoundException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SigningException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongSignatureException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongSignatureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			/*catch (WebServiceException e) {
			checkWebServiceException(e);
		}*/
			
		}
    }
    
    public byte[] get(byte[] domain, byte[] username) throws NotInitializedException {
    	if(publicKey == null || symmetricKey == null)
			throw new NotInitializedException();
    	
    	byte[] retrivedPassword = null;
    	
		byte[] iv = createIV(domain, username);
		byte[] cDomain = cipherWithSymmetric(symmetricKey, domain, iv);
		byte[] cUsername = cipherWithSymmetric(symmetricKey,username, iv);
		
		List<List<Object>> ackList = new ArrayList<List<Object>>();
		
		// Makes the get
		for(ByzantineRegisterConnection brc : conns) {
			Thread aux = new Thread(new SendGet(brc, deviceID, cDomain, cUsername, ackList));
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
		
		return retrivedPassword;
    }
    
    // TODO: Check the expections in a better way!!!
    
    private class SendGet implements Runnable {
    	private ByzantineRegisterConnection brc;
    	private byte[] deviceID;
    	private byte[] cDomain;
    	private byte[] cUsername;
    	
    	// It will store the received wTS
    	private List<List<Object>> ackList;
    	
		public SendGet(ByzantineRegisterConnection brc, byte[] deviceID, byte[] cDomain, byte[] cUsername, List<List<Object>> ackList) { 
			this.brc = brc;
			this.deviceID = deviceID;
			this.cDomain = cDomain;
			this.cUsername = cUsername;
			this.ackList = ackList;
		}

		@Override
		public void run() {
			try {
				List<Object> res = brc.get(deviceID, cDomain, cUsername);
				
				
				// TODO: This signature check is wrong and should be updated!!! 
				
//				result = new ArrayList<Object>();
//				result.add(retrivedPassword);
//				result.add(wTS);
//				result.add(deviceIDWr);
//				result.add(clientSig);
				
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
			} catch (SigningException | WrongSignatureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnregisteredUserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyConversionException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoPasswordException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoPublicKeyException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NullArgException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SessionNotFoundException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SigningException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongSignatureException_Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
}
