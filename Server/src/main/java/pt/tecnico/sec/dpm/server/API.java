package pt.tecnico.sec.dpm.server;

import java.util.List;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.security.exceptions.*;
import pt.tecnico.sec.dpm.server.exceptions.NoPasswordException;
import pt.tecnico.sec.dpm.server.exceptions.NoPublicKeyException;
import pt.tecnico.sec.dpm.server.exceptions.NullArgException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInvalidSizeException;

@WebService
public interface API {
	byte[] register(byte[] publicKey, byte[] sig) throws PublicKeyInUseException, NullArgException,
	PublicKeyInvalidSizeException, KeyConversionException, WrongSignatureException, SigningException;
	
	// The return will have: nonce + 1, sessionID, sig
	// TODO: Add some exceptions!!!
	List<byte[]> login(byte[] publicKey, byte[] nonce, byte[] sig);
	
	// The return will have: (int) counter + 1, (byte[]) sig
	List<Object> put(byte[] sessionID, int counter, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig) throws NoPublicKeyException, NullArgException;
	
	// The return will have: (int) counter + 1, (byte[]) password, (int) wTS, (byte[]) serverSig, (int) wrCounter, (byte[]) clientSig
	List<Object> get(byte[] sessionID, int counter, byte[] domain, byte[] username, byte[] sig) throws NoPasswordException, NullArgException, NoPublicKeyException;
}
