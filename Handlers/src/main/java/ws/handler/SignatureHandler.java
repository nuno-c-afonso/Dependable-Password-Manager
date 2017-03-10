package ws.handler;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.lang.System;

//import pt.ca.cli.*;

public class SignatureHandler implements SOAPHandler<SOAPMessageContext> {

    public static final String CONTEXT_PROPERTY = "my.property";
    public static final String MYNAME= "my.myname.property";
    public static final String OTHERSNAME= "my.othersname.property";
    public static final String PRIVATEKEY = "my.privatekey.property";
    public static final String SYMMETRICKEY ="my.symmetrickey.property";
    public static final String PASSWORDKEYSTORE ="my.passwordKeystore.property";
    public static final String PASSWORDKEYS ="my.passwordKeys.property";


    Random randomGenerator = new Random();

    protected int nonce=0;
    protected long timestamp;
    private String myName=null;
    private String othersName=null;
    String firstReceivedOriginName=null;
    String firstReceivedDestinationName=null;
    
    private static PrivateKey myprivateKey = null;
    private char[] passwordKeystore = null;
    private char[] passwordKeys = null;
    


    public Set<QName> getHeaders() {
        return null;
    }

	@Override
	public boolean handleMessage(SOAPMessageContext context) {

		 Boolean outboundElement = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		 if(outboundElement.booleanValue()) {
			 return handleRequest(context);
		 }
		 else
			 return handleResponse(context);

	}

    public boolean handleRequest(MessageContext context){
    	//Initialize the the variables if needed
    	try {
    		if(myName==null)
    			myName = (String) context.get(MYNAME);
    		if(othersName==null)
    			othersName = ((String) context.get(OTHERSNAME)).toLowerCase().replace('/','0');
    		if(myprivateKey==null)
    			myprivateKey = (PrivateKey) context.get(PRIVATEKEY);
    		if(passwordKeys==null)
    			passwordKeys = (char[]) context.get(PASSWORDKEYS);
    		if(passwordKeystore==null)
    			passwordKeystore = (char[]) context.get(PASSWORDKEYSTORE);

    		
    		//Dissecate the message
    		SOAPMessageContext smc = (SOAPMessageContext) context;
    		SOAPMessage msg = smc.getMessage();
    		SOAPPart sp = msg.getSOAPPart();
    		SOAPEnvelope se = sp.getEnvelope();
    		SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
    		if (sh == null) {sh = se.addHeader();}

    		//Add the destination of the message to the header
    		Name name = se.createName("destinationName","d","http://demo");
    		SOAPHeaderElement element = sh.addHeaderElement(name);
    		element.addTextNode(othersName);
    		
    		//add timestamp to the header to check freshness
    		name = se.createName("timeStamp","d","http://demo");
    		element = sh.addHeaderElement(name);
    		String stringTimestamp = Long.toString(System.currentTimeMillis());
    		element.addTextNode(stringTimestamp);

    		//concat everyting to  sign
    		String toDigest = sb.getTextContent();
    		toDigest= toDigest.concat(othersName);
    		toDigest= toDigest.concat(stringTimestamp);

    		String toSend=makeDigitalSignature(toDigest);
    		if(toSend==null){
    			System.out.println("Error while signing");
    			return false;
    		}
    		
    		//add resume to the header
    		name = se.createName("resume","d","http://demo");
    		element = sh.addHeaderElement(name);
    		element.addTextNode(toSend);
    	}catch(Exception e){System.out.println(e);}
    	
    	return true;
    }





    public boolean handleResponse(MessageContext context){
    	try {
    		//Dissecate the message
    		SOAPMessageContext smc = (SOAPMessageContext) context;
    		SOAPMessage msg = smc.getMessage();
    		SOAPPart sp = msg.getSOAPPart();
    		SOAPEnvelope se = sp.getEnvelope();
    		SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
    		if (sh == null) {sh = se.addHeader();}

    		//take the received timestamp
    		Name timeName = se.createName("timeStamp","d","http://demo");
    		Iterator it =sh.getChildElements(timeName);
    		if (!it.hasNext()) {
                System.out.println("Nonce header element not found.");
                makeFault(sb,"missingTimestamp","Timestamp header element not found.");
                return false;
            }
    		SOAPElement timeElement = (SOAPElement) it.next();
    		String timeValue = timeElement.getValue();
    		timestamp = Long.parseLong(timeValue);
    		if(!checkTimestamp(timestamp)){
    			System.out.println("Timestamp not valid");
    			makeFault(sb,"wrongTimestamp","Timestamp not valid");
    			return false;
    		}
    		
    		//take the received resume
    		Name resumeName = se.createName("resume","d","http://demo");
    		it =sh.getChildElements(resumeName);
    		if (!it.hasNext()) {
                System.out.println("Resume header element not found.");
                makeFault(sb,"missingResume","Resume header element not found.");
                return false;
            }
            SOAPElement resumeElement = (SOAPElement) it.next();
    		String signedResume = resumeElement.getValue();

    		String toDigest=sb.getTextContent();
    		toDigest= toDigest.concat(timeValue);

    		// Check if the Signature of the received content is correct
    		boolean signatureAutentic = checkSignature(othersName,myName,toDigest,signedResume);
    		if(!signatureAutentic){
    			System.out.println("!!!The signature isnt ok");
    			makeFault(sb,"ViolatedMessage","The signature check FAILED");
    		}

    		sh.detachNode();
    		return signatureAutentic;

        }catch(Exception e){System.out.println(e.getMessage());}
    	return false;
    }

    public void makeFault(SOAPBody sb,String faultNamein,String faultText) throws SOAPException{
		sb.removeContents();
		SOAPFault fault = sb.addFault();
		QName faultName = new QName(faultNamein);
		fault.setFaultCode(faultName);
		fault.setFaultString(faultText);
    }


    private boolean checkSignature(String otherName,String myName ,String text,String signedText){

    	final byte[] plainBytesText = parseBase64Binary(text);
    	final byte[] plainBytesSignedText = parseBase64Binary(signedText);
    	
    	
		String currentDir=null;
		try {currentDir = new java.io.File( "." ).getCanonicalPath();
		} catch (IOException e1) {e1.printStackTrace();}

		KeyStore ks=null;
		try {ks = KeyStore.getInstance("jceks");
		} catch (KeyStoreException e1) {//e1.printStackTrace();
			}

		//Load the keystore from the filesystem
		String path=currentDir + "/../keys/allcerts/allcerts.jks";
		try {ks.load(new FileInputStream(path), passwordKeystore);
		} catch (NoSuchAlgorithmException | CertificateException | IOException e1) {e1.printStackTrace();}


		//Get the certeficate from the Keysore
		X509Certificate certificate=null;
		try {certificate = (X509Certificate) ks.getCertificate(otherName.toLowerCase().replace('/','0'));
		} catch (KeyStoreException e1) {e1.printStackTrace();}
		if(certificate==null){
			System.out.println("Does not have certificate of: "+ otherName);
			return false;
		}
		
		
		//Verify the signature with the corresponding public key
        try {
        	Signature sig = Signature.getInstance("SHA256WithRSA");
        	sig.initVerify(certificate);
        	sig.update(plainBytesText);
            return sig.verify(plainBytesSignedText);
        } catch (SignatureException se) {
        	System.out.println("Exception verifying certeficate");
        	se.printStackTrace();
            return false;
        } catch (Exception e){ System.out.println("Exception veryfying certeficate"+e);
        System.out.println("Exception veryfying certeficate"+ e);}
        return false;
    }


    public static String makeDigitalSignature(String text) throws Exception {

    	byte[] plainBytesText;
    	try{
    		plainBytesText = parseBase64Binary(text);
    	}catch(Exception e){
    		System.out.println("erro a passar para bytes"+e);
    		return null;
    	}
    	Signature sig = Signature.getInstance("SHA256WithRSA");
    	sig.initSign((PrivateKey) myprivateKey);
    	sig.update(plainBytesText);
    	byte[] signature = sig.sign();

    	return printBase64Binary(signature);
    }

    public boolean checkTimestamp(long sendTime){
    	long now = System.currentTimeMillis()/1000;
    	long send = sendTime/1000;
    	return now - send < 5;//TODO CHECK THIS VALUE
    }


    public boolean handleFault(SOAPMessageContext smc) {
        System.out.println("Ignoring fault message...");
        SOAPBody sb=null;
        SOAPFault fault;
        try{
	        SOAPMessage msg = smc.getMessage();
			SOAPPart sp = msg.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			sb = se.getBody();
        }catch(Exception e){}
        if(sb!=null){
        	fault =sb.getFault();
        	String faultCode =fault.getFaultCode();
        	if(faultCode.equals("missingDestination")||
        			faultCode.equals("missingTimestamp")||
        			faultCode.equals("wrongTimestamp")||
        			faultCode.equals("missingResume")||
        			faultCode.equals("ViolatedMessage")||
        			faultCode.equals("WrongEndPoints")
        			)
        	throw new SOAPFaultException(sb.getFault());
        }
        return true;
    }

    public void close(MessageContext messageContext) {
    }







}
