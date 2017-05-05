package pt.tecnico.sec.dpm.broadcastClient;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.Scanner;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;
import pt.tecnico.sec.dpm.server.broadcastClient.BroadcastClient;


//import pt.tecnico.sec.dpm.client.DpmClient;


public class ClientApplication{
	public static void main(String[] args) throws Exception{
		// Check arguments
		if (args.length < 2) {
			System.err.println("Argument missing!");
			System.err.printf("Usage: java %s wsURL wsURL ... wsURL ", ClientApplication.class.getName());
			return;
		}
		
		int argsSize = args.length;
		String urls[] = Arrays.copyOfRange(args, 0, argsSize);
			
		BroadcastClient client = new BroadcastClient(urls);
		Scanner scanner = new Scanner(System.in);
		
		scanner.close();		
		System.out.println("Goodbye.");
		
	}
}
	





