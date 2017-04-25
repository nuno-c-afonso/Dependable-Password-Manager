package pt.tecnico.sec.dpm.security;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.lang.System;


public class SecurityFunctions  {

    private static boolean checkSignature(PublicKey publicKey, String text, String signedText){
    	final byte[] plainBytesText = parseBase64Binary(text);
    	final byte[] plainBytesSignedText = parseBase64Binary(signedText);
    	
        try {
        	Signature sig = Signature.getInstance("SHA256WithRSA");
        	sig.initVerify(publicKey);
        	sig.update(plainBytesText);
            return sig.verify(plainBytesSignedText);
            
        } catch (SignatureException se) {
        	System.out.println("Client Exception verifying certeficate"+se);
        	se.printStackTrace();
            return false;
        } catch (Exception e){ System.out.println("Exception veryfying certeficate"+e);
        System.out.println("Exception veryfying certeficate"+ e);}
        return false;
    }


    public static String makeDigitalSignature(PrivateKey privatekey, String text) throws Exception {

    	byte[] plainBytesText;
    	try{
    	plainBytesText = parseBase64Binary(text);
    	}catch(Exception e){
    		System.out.println("erro a passar para bytes"+e);
    		e.printStackTrace();
    		return null;
    	}
    	Signature sig = Signature.getInstance("SHA256WithRSA");
    	sig.initSign(privatekey);
    	sig.update(plainBytesText);
    	byte[] signature = sig.sign();

    	return printBase64Binary(signature);
    }

}
