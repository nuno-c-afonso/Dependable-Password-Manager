package pt.tecnico.sec.dpm.server;

import java.util.List;

import javax.jws.WebService;

import pt.tecnico.sec.dpm.security.exceptions.*;
import pt.tecnico.sec.dpm.server.exceptions.DuplicatedNonceException;
import pt.tecnico.sec.dpm.server.exceptions.NoPasswordException;
import pt.tecnico.sec.dpm.server.exceptions.NoPublicKeyException;
import pt.tecnico.sec.dpm.server.exceptions.NullArgException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInUseException;
import pt.tecnico.sec.dpm.server.exceptions.PublicKeyInvalidSizeException;
import pt.tecnico.sec.dpm.server.exceptions.SessionNotFoundException;

@WebService
public interface API {
	byte[] register(byte[] publicKey, byte[] sig) throws PublicKeyInUseException, NullArgException,
	PublicKeyInvalidSizeException, KeyConversionException, WrongSignatureException, SigningException;
	
	// The return will have: nonce + 1, sessionID, sig
	// TODO: Add some exceptions!!!
	List<Object> login(byte[] publicKey, byte[] nonce, byte[] sig) throws SigningException,
	KeyConversionException, WrongSignatureException, NullArgException, NoPublicKeyException, DuplicatedNonceException;
	
	// The return will have: (int) counter + 1, (byte[]) sig
	List<Object> put(int sessionID, int counter, byte[] domain, byte[] username, byte[] password, int wTs, byte[] sig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException;
	
	// The return will have: (int) counter + 1, (byte[]) password, (int) wTS, (byte[]) serverSig, (int) wrCounter, (byte[]) clientSig
	List<Object> get(int sessionID, int counter, byte[] domain, byte[] username, byte[] sig)
			throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException;
}
