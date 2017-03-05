package pt.tecnico.sec.dpm.client;
import java.security.KeyStore;

public class DpmClient {

	public void init(KeyStore keystore ){
		//TODO
	}
	
	
	public void init(KeyStore keystore, char[] password ){
		//TODO
	}
	
	public void register_user(){
		//TODO
	}
	
	public void save_password(byte[] domain, byte[] username, byte[] password){
		//TODO
	}
	
	public byte[] retrieve_password(byte[] domain, byte[] username){
		//TODO
		return "change me".getBytes();
	}
	
	public void close(){
		//TODO
	}
	
}
