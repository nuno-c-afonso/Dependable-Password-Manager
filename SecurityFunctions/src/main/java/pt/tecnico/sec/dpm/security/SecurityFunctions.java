package pt.tecnico.sec.dpm.security;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import pt.tecnico.sec.dpm.security.exceptions.KeyConversionException;
import pt.tecnico.sec.dpm.security.exceptions.SigningException;
import pt.tecnico.sec.dpm.security.exceptions.WrongSignatureException;

import java.lang.System;
import java.math.BigInteger;
import java.nio.ByteBuffer;


public class SecurityFunctions  {
	
	/*
	 * Checks the given signature
	 */
	
    public static void checkSignature(PublicKey publicKey, String text, String signedText) throws WrongSignatureException {
    	final byte[] plainBytesText = parseBase64Binary(text);
    	final byte[] plainBytesSignedText = parseBase64Binary(signedText);
    	checkSignature(publicKey, plainBytesText, plainBytesSignedText);
    }
    
    public static void checkSignature(PublicKey publicKey, String text, byte[] signedText) throws WrongSignatureException {
    	final byte[] plainBytesText = parseBase64Binary(text);
    	checkSignature(publicKey, plainBytesText, signedText);
    }
    
    public static void checkSignature(PublicKey publicKey, byte[] text, String signedText) throws WrongSignatureException {
    	final byte[] plainBytesSignedText = parseBase64Binary(signedText);
    	checkSignature(publicKey, text, plainBytesSignedText);
    }
    
    public static void checkSignature(PublicKey publicKey, byte[] text, byte[] signedText) throws WrongSignatureException {
        try {
        	Signature sig = Signature.getInstance("SHA256WithRSA");
        	sig.initVerify(publicKey);
        	sig.update(text);
        	if(!sig.verify(signedText))
        		throw new WrongSignatureException();
        } catch (Exception e) {
        	throw new WrongSignatureException();
        }
    }

    /*
     * Makes the signature
     */
    
    public static byte[] makeDigitalSignature(PrivateKey privatekey, String text) throws SigningException {
    	try{
    		byte[] plainBytesText = parseBase64Binary(text);
    		return makeDigitalSignature(privatekey, plainBytesText);
    	} catch(Exception e){
    		throw new SigningException();
    	}
    }
    
    public static byte[] makeDigitalSignature(PrivateKey privatekey, byte[] text) throws SigningException {
    	byte[] signature = null;
    	
    	try {
    		Signature sig = Signature.getInstance("SHA256WithRSA");
    		sig.initSign(privatekey);
        	sig.update(text);
        	signature = sig.sign();
    	} catch(Exception e) {
    		throw new SigningException();
    	}

    	return signature;
    }
    
    /*
     * Key conversions
     */
    
    public static byte[] keyToByteArray(Key k) throws KeyConversionException {
    	byte[] b = k.getEncoded();
    	
    	if(b == null)
    		throw new KeyConversionException();
    	return b;
    }
    
    public static PublicKey byteArrayToPubKey(byte[] b) throws KeyConversionException{
    	PublicKey k = null;
    	
    	try {
			KeyFactory kf = KeyFactory.getInstance("RSA");
			k = kf.generatePublic(new X509EncodedKeySpec(b));
    	} catch(Exception e) {
    		throw new KeyConversionException();
    	}
    	
    	return k;
    }
    
    public static PrivateKey byteArrayToPrivKey(byte[] b) throws KeyConversionException{
    	PrivateKey k = null;
    	
    	try {
			KeyFactory kf = KeyFactory.getInstance("RSA");
			k = kf.generatePrivate(new PKCS8EncodedKeySpec(b));
    	} catch(Exception e) {
    		throw new KeyConversionException();
    	}
    	
    	return k;
    }
    
    /*
     * Aux functions
     */
	
	public static byte[] concatByteArrays(byte[]... arrays) {
		int newSize = 0;
		int counterSize = 0;
		
		for(byte[] el : arrays)
			newSize += el.length;
		
		byte[] result = new byte[newSize];
		for(byte[] el : arrays) {
			int elSize = el.length;
			System.arraycopy(el, 0, result, counterSize, elSize);
			counterSize += elSize;
		}
		
		return result;
	}
	
	// Some extra methods for type conversion
	public static int byteArrayToInt(byte[] bytes) {
	     return new BigInteger(bytes).intValue();
	}
	
	public static byte[] intToByteArray(int n) {
		int ARRAY_SIZE = 64;
		ByteBuffer b = ByteBuffer.allocate(ARRAY_SIZE + 1);
		b.putInt(n);
		return b.array();
	}
}
