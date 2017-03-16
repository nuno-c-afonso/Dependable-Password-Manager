package ws.handler;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.Base64;

import javax.crypto.SecretKey;
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

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.lang.System;

//import pt.ca.cli.*;

public class ServerSignatureHandler implements SOAPHandler<SOAPMessageContext> {

    public static final String CONTEXT_PROPERTY = "my.property";
    public static final String MYNAME= "my.myname.property";
    public static final String OTHERSNAME= "my.othersname.property";
    public static final String PRIVATEKEY = "my.privatekey.property";


    Random randomGenerator = new Random();

    protected int nonce=0;
    protected long timestamp;
    private String myName=null;
    private String othersName=null;
    String firstReceivedOriginName=null;
    String firstReceivedDestinationName=null;
    String prevPubKey = null;
    
    private static PrivateKey myprivateKey = null;


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
    	try {
    		if(myName==null)
    			myName = (String) context.get(MYNAME);
    		if(othersName==null)
    			othersName = (String) context.get(OTHERSNAME);
    		if(myprivateKey==null)
    			myprivateKey = (PrivateKey) context.get(PRIVATEKEY);

    		
    		SOAPMessageContext smc = (SOAPMessageContext) context;
    		SOAPMessage msg = smc.getMessage();
    		SOAPPart sp = msg.getSOAPPart();
    		SOAPEnvelope se = sp.getEnvelope();
    		SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
    		if (sh == null) {sh = se.addHeader();}
    		
    		//add timestamp to the header
    		Name name = se.createName("timeStamp","d","http://demo");
    		SOAPHeaderElement element = sh.addHeaderElement(name);
    		String stringTimestamp = Long.toString(System.currentTimeMillis());
    		element.addTextNode(stringTimestamp);

    		//concat everyting to make sign
    		String toDigest = sb.getTextContent();
    		toDigest= toDigest.concat(stringTimestamp);
    		toDigest = toDigest.concat("-");
    		toDigest = toDigest.concat(prevPubKey);
    		toDigest = toDigest.concat("-");
    		
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

    		SOAPMessageContext smc = (SOAPMessageContext) context;
    		SOAPMessage msg = smc.getMessage();
    		SOAPPart sp = msg.getSOAPPart();
    		SOAPEnvelope se = sp.getEnvelope();
    		SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
    		if (sh == null) {sh = se.addHeader();}

    		//take destination name
    		Name destinationname = se.createName("destinationName","d","http://demo");
    		Iterator it =sh.getChildElements(destinationname);
    		if (!it.hasNext()) {
                System.out.println("Destination header element not found.");
                makeFault(sb,"missingDestination","Destination header element not found.");
                return false;
            }
            SOAPElement destinationElement = (SOAPElement) it.next();
    		String destinationName = destinationElement.getValue();

    		//take the received timestamp
    		Name timeName = se.createName("timeStamp","d","http://demo");
    		it =sh.getChildElements(timeName);
    		if (!it.hasNext()) {
                System.out.println("timeStamp header element not found.");
                makeFault(sb,"missingTimestamp","Timestamp header element not found.");
                return false;
            }
    		SOAPElement timeElement = (SOAPElement) it.next();
    		String timeValue = timeElement.getValue();
    		timestamp = Long.parseLong(timeValue);
    		//check if the time is in the expected interval for freshness
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
    		toDigest= toDigest.concat(destinationName);
    		toDigest= toDigest.concat(timeValue);


    		if (myName != null){
	    		if(!myName.equals(destinationName)){
	    			System.out.println("!!! Received Message  with  destination"+destinationName+" but i am:"+myName +" !");
	    			makeFault(sb,"WrongEndPoints","!!! Received Message with wrong destination !!!");
	    			return false;
	    		}
    		}
    		
    		SOAPElement method =(SOAPElement) sb.getFirstChild();
    		Name key = se.createName("arg0");
            it =method.getChildElements(key);
            if (!it.hasNext()) {
            	System.out.println("nao encontra o key");
            	makeFault(sb,"ClientePublicKeyNotFound","!!! Received Message without publicKey !!!");
                return false;
            } 
            
            //get the client PUBLIC KEY from the message
            SOAPElement pubkey = (SOAPElement) it.next();
            prevPubKey = pubkey.getValue();
            byte[] arraykey= Base64.getDecoder().decode(prevPubKey);
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(arraykey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);
    		
            boolean signatureAutentic = checkSignature(publicKey,toDigest,signedResume);
    		if(!signatureAutentic){
    			System.out.println("!!!The signature isnt ok");
    			makeFault(sb,"ViolatedMessage","The signature check FAILED");
    		}

    		sh.detachNode();
    		return signatureAutentic;

        }catch(Exception e){System.out.println("found it"+e.getMessage());e.printStackTrace();}
    	return false;
    }

    public void makeFault(SOAPBody sb,String faultNamein,String faultText) throws SOAPException{
		sb.removeContents();
		SOAPFault fault = sb.addFault();
		QName faultName = new QName(faultNamein);
		fault.setFaultCode(faultName);
		fault.setFaultString(faultText);
    }



    private boolean checkSignature(PublicKey publicKey  ,String text,String signedText){
    	
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


    public static String makeDigitalSignature(String text) throws Exception {

    	byte[] plainBytesText;
    	try{
    	plainBytesText = parseBase64Binary(text);
    	}catch(Exception e){
    		System.out.println("erro a passar para bytes"+e);
    		e.printStackTrace();
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
    	long offset = now - send;
    	return offset < 5 && offset > -2;//TODO CHECK THIS VALUE
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
        			faultCode.equals("WrongEndPoints")||
        			faultCode.equals("ClientePublicKeyNotFound")
        			)
        	throw new SOAPFaultException(sb.getFault());
        }
        return true;
    }

    public void close(MessageContext messageContext) {
    }

}
