package pt.tecnico.sec.dpm.client;
import java.security.KeyStore;
import java.security.KeyStoreException;

//import pt.tecnico.sec.dpm.client.DpmClient;


public class ClientApplication{
	public static void main(String[] args){
		KeyStore ks= null;
		try{
			 ks = KeyStore.getInstance(KeyStore.getDefaultType());
		}catch (KeyStoreException e){System.out.print(e.getMessage()); 
		
		}
		
		DpmClient client = new DpmClient();
		
		client.init(ks);
		
		client.register_user();
		
		client.save_password("domain".getBytes(), "username".getBytes(), "password".getBytes());
		
		client.retrieve_password("domain".getBytes(), "username".getBytes());
		
		client.close();
		
		
		
	}
	
}
	





