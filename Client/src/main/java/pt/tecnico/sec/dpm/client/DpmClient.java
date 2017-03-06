package pt.tecnico.sec.dpm.client;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

public class DpmClient {
	
	public PublicKey publicKey = null;
	public PrivateKey privateKey = null;
	public SecretKey symmetricKey = null;


	
	
	public void init(KeyStore keystore, char[] passwordKeystore, char[] passworKeys ){
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
