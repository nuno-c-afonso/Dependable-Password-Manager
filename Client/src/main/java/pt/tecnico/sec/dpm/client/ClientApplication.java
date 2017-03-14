package pt.tecnico.sec.dpm.client;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.InputMismatchException;
import java.util.Scanner;

import pt.tecnico.sec.dpm.client.exceptions.AlreadyInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.ConnectionWasClosedException;
import pt.tecnico.sec.dpm.client.exceptions.GivenAliasNotFoundException;
import pt.tecnico.sec.dpm.client.exceptions.NotInitializedException;
import pt.tecnico.sec.dpm.client.exceptions.NullClientArgException;
import pt.tecnico.sec.dpm.client.exceptions.NullKeystoreElementException;
import pt.tecnico.sec.dpm.client.exceptions.UnregisteredUserException;
import pt.tecnico.sec.dpm.client.exceptions.WrongPasswordException;
import pt.tecnico.sec.dpm.server.NoPasswordException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInUseException_Exception;
import pt.tecnico.sec.dpm.server.PublicKeyInvalidSizeException_Exception;

//import pt.tecnico.sec.dpm.client.DpmClient;


public class ClientApplication{
	public static void main(String[] args){
		KeyStore keystore = null;
		
		// Check arguments
		if (args.length == 0) {
			System.err.println("Argument missing!");
			System.err.printf("Usage: java %s wsURL%n", ClientApplication.class.getName());
			return;
		}
		
		String url = args[0];
		
        try {
        	keystore = KeyStore.getInstance("jceks");
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
        
        java.io.FileInputStream file = null;
        char[] passwordFile = "ins3cur3".toCharArray();
        try {
        	file = new java.io.FileInputStream("../keys/client/client.jks");
			keystore.load(file,passwordFile);
		} catch (NoSuchAlgorithmException e1) {	e1.printStackTrace();
		} catch (CertificateException e1) { e1.printStackTrace();
		} catch (IOException e1) { e1.printStackTrace();
		} finally {
	        if (file != null) {
	            try {
					file.close();
				} catch (IOException e) { e.printStackTrace();}
	        }
	    }		
		
		DpmClient client = new DpmClient(url);
		Scanner scanner = new Scanner(System.in);
		
		try {
			client.init(keystore, "ins3cur3".toCharArray(),"client", "secretKey", "1nsecure".toCharArray());
			
			boolean cont = true;
			while(cont) {
				System.out.println("Please select the desired option:");
				System.out.println("1 - Register user;");
				System.out.println("2 - Save password;");
				System.out.println("3 - Retrieve password;");
				System.out.println("4 - Close.");
				System.out.print("> ");
				
				int option = -1;
				
				try {
					option = scanner.nextInt();
				} catch(InputMismatchException e) {
					// It will be treated on the default case
					scanner.next(); // It cleans the input
				}
				
				String domain, username, password;				
				switch(option) {
					case 1:
					try {
						client.register_user();
					} catch (PublicKeyInUseException_Exception e1) {
						System.out.println(e1.getMessage());
					}
						break;
					case 2:
						System.out.print("Domain: ");
						domain = scanner.next();
						System.out.print("Username: ");
						username = scanner.next();
						System.out.print("Password: ");
						password = scanner.next();
					try {
						client.save_password(domain.getBytes(), username.getBytes(), password.getBytes());
					} catch (NullClientArgException | UnregisteredUserException e1) {
						System.out.println(e1.getMessage());
					}
						break;
					case 3:
						System.out.print("Domain: ");
						domain = scanner.next();
						System.out.print("Username: ");
						username = scanner.next();
					try {
						password = new String(client.retrieve_password(domain.getBytes(), username.getBytes()));
						System.out.println("Recovered password: " + password);
					} catch (NoPasswordException_Exception | NullClientArgException | UnregisteredUserException e) {
						System.out.println(e.getMessage());
					}
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
				| WrongPasswordException | NotInitializedException | PublicKeyInvalidSizeException_Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConnectionWasClosedException e) {
			System.out.println("Could not reach the server. Please try again later.");
		}
		
		scanner.close();
		
		try {
			System.out.println("Goodbye.");
			client.close();
		} catch (NotInitializedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
}
	





