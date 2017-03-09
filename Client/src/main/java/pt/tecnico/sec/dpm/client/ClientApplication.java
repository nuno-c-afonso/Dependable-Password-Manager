package pt.tecnico.sec.dpm.client;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Scanner;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.GivenAliasNotFoundException;
import pt.tecnico.sec.dpm.client.exceptions.NotInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.NullKeystoreElementException;
import pt.tecnico.sec.dpm.client.exceptions.WrongPasswordException;

//import pt.tecnico.sec.dpm.client.DpmClient;


public class ClientApplication{
	public static void main(String[] args){
		
		KeyStore keystore = null;
		KeyGenerator keyGenAES = null;
		SecretKey symmetricKey = null;
		
		try{	keyGenAES = KeyGenerator.getInstance("AES");
    	}catch(NoSuchAlgorithmException e){System.out.print(e.getMessage());}

        try {  	keystore = KeyStore.getInstance("jceks");
		} catch (KeyStoreException e) {e.printStackTrace();}
        
        java.io.FileInputStream file = null;
        char[] passwordFile = "ins3cur3".toCharArray();
        try {
        	file = new java.io.FileInputStream("keys/client/client.jks");
			keystore.load(file,passwordFile);
		} catch (NoSuchAlgorithmException e1) {	e1.printStackTrace();
		} catch (CertificateException e1) { e1.printStackTrace();
		} catch (IOException e1) { e1.printStackTrace();
		}finally {
	        if (file != null) {
	            try {
					file.close();
				} catch (IOException e) { e.printStackTrace();}
	        }
	    }
        
        
		try {symmetricKey =  (SecretKey) keystore.getKey("secretKey", "1nsecure".toCharArray());
		} catch (KeyStoreException e1) {e1.printStackTrace();System.out.println("1");
		} catch (UnrecoverableKeyException e) { e.printStackTrace();System.out.println("2");
		} catch (NoSuchAlgorithmException e) { e.printStackTrace();System.out.println("3");}
		
		
		
		if (symmetricKey == null){
			System.out.println("tava null");
			//System.out.println(symmetricKey.toString());
			char[] pass = "1nsecure".toCharArray();
	        KeyStore.ProtectionParameter protParam = new KeyStore.PasswordProtection(pass);
	        keyGenAES.init(256);
	        symmetricKey = keyGenAES.generateKey();
	        
	        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(symmetricKey);
	        try {keystore.setEntry("secretKey", skEntry, protParam);
			} catch (KeyStoreException e) {e.printStackTrace();System.out.println("4");}
			
	        java.io.FileOutputStream file2 = null;
	        try {
	        	file2 = new java.io.FileOutputStream("keys/client/client.jks");
				keystore.store(file2, passwordFile);
			} catch (NoSuchAlgorithmException e1) {	e1.printStackTrace();
			} catch (CertificateException e1) { e1.printStackTrace();
			} catch (IOException e1) { e1.printStackTrace();
			} catch (KeyStoreException e) { e.printStackTrace();
			}finally {
		        if (file != null) {
		            try {
						file.close();
					} catch (IOException e) { e.printStackTrace();}
		        }
		    }
		}
		
		
		
		
		
		DpmClient client = new DpmClient("http://localhost:8080/ws.API/endpoint");
		Scanner scanner = new Scanner(System.in);
		
		//TODO: Change the null entry by the server's certificate name
		try {
			client.init(keystore, "ins3cur3".toCharArray(),"client", "secretKey", "DELETE THIS", "1nsecure".toCharArray());
			
			boolean cont = true;
			while(cont) {
				System.out.println("Please select the desired option:");
				System.out.println("1 - Register user;");
				System.out.println("2 - Save password;");
				System.out.println("3 - Retrieve password;");
				System.out.println("4 - Close.");
				
				int option = scanner.nextInt();
				String domain, username, password;				
				switch(option) {
					case 1:
						client.register_user();
						break;
					case 2:
						System.out.println("Domain: ");
						domain = scanner.next();
						System.out.println("Username: ");
						username = scanner.next();
						System.out.println("Password: ");
						password = scanner.next();
						client.save_password(domain.getBytes(), username.getBytes(), password.getBytes());
						break;
					case 3:
						System.out.println("Domain: ");
						domain = scanner.next();
						System.out.println("Username: ");
						username = scanner.next();
						password = new String(client.retrieve_password(domain.getBytes(), username.getBytes()));
						System.out.println("Recovered password: " + password);
						break;
					case 4:
						cont = false;
						break;
					default:
						System.out.println("Cannot recognize it. Please try again.");
						break;
				}
			}
			
			
			
			
		} catch (AlreadyInitializedException | NullKeystoreElementException | GivenAliasNotFoundException
				| WrongPasswordException | NotInitializedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		try {
			System.out.println("Goodbye.");
			client.close();
		} catch (NotInitializedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
	





