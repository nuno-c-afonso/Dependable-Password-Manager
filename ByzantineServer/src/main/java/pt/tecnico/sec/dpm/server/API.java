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
	
	// The return will have: byte[] with {"login" || deviceID || nonce || 1}SPriv
	byte[] login(byte[] publicKey, byte[] deviceID, byte[] nonce, byte[] sig) throws SigningException,
	KeyConversionException, WrongSignatureException, NullArgException, NoPublicKeyException, DuplicatedNonceException;
	
	// The return will have: byte[] with {"put" || deviceID || nonce || counter + 1}SPriv
	byte[] put(byte[] deviceID, byte[] nonce, byte[] domain, byte[] username, byte[] password, int wTs, byte[] bdSig, int counter, byte[] sig)
			throws NoPublicKeyException, NullArgException, SessionNotFoundException, KeyConversionException, WrongSignatureException, SigningException;
	
	// The return will have: password, w_ts, deviceID_wr, XXX,
	// {"get" || deviceID || nonce || counter + 1 || password || w_ts || deviceID_wr, XXX}SPriv
	List<Object> get(byte[] deviceID, byte[] nonce, byte[] domain, byte[] username, int counter, byte[] sig)
			throws NoPasswordException, NullArgException, NoPublicKeyException, SessionNotFoundException, KeyConversionException,
			WrongSignatureException, SigningException;
}
