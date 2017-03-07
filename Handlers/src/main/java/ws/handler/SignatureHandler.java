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
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.Normalizer;

import pt.ca.cli.*;

public class SignatureHandler implements SOAPHandler<SOAPMessageContext> {

    public static final String CONTEXT_PROPERTY = "my.property";
    public static final String MYNAME= "my.myname.property";
    public static final String OTHERSNAME= "my.othersname.property";

    Random randomGenerator = new Random();

    protected int nonce=0;
    protected int nonceReceived;
    private String myName=null;
    private String othersName=null;
    String firstReceivedOriginName=null;
    String firstReceivedDestinationName=null;
    boolean needToCheckNames =false; /*used to protect the vulnerability of the first message received
     								by the server because the response handler will not have
     								access to the context variables that are set in the web service
     								1 is yes*/

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
    	if (needToCheckNames){
    		 System.out.println("needToCheckNames");		//TODO
    		needToCheckNames=false;
    	}

    	//System.out.println(this + ">\n\t handleRequest(MessageContext=" + context+ ")");
    	try {
    		//System.out.println("handling Request");
    		if(myName==null)
    			myName = (String) context.get(MYNAME);
    		if(othersName==null)
    			othersName = (String) context.get(OTHERSNAME);

    		SOAPMessageContext smc = (SOAPMessageContext) context;
    		SOAPMessage msg = smc.getMessage();
    		SOAPPart sp = msg.getSOAPPart();
    		SOAPEnvelope se = sp.getEnvelope();
    		SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
    		if (sh == null) {sh = se.addHeader();}


    		//TODO verificar este namespace
    		//add the name of the sender
    		Name name = se.createName("senderName","d","http://demo");
    		SOAPHeaderElement element = sh.addHeaderElement(name);
    		element.addTextNode(myName);
    		//add the name of the destination
    		name = se.createName("destinationName","d","http://demo");
    		element = sh.addHeaderElement(name);
    		element.addTextNode(othersName);
    		//add nonce to the header
    		name = se.createName("nonce","d","http://demo");
    		element = sh.addHeaderElement(name);
    		String stringNonce = Integer.toString(getNonce());
    		element.addTextNode(stringNonce);

    		//concat everyting to make sign
    		String toDigest=sb.getTextContent();
    		toDigest= toDigest.concat(myName);
    		toDigest= toDigest.concat(othersName);
    		toDigest= toDigest.concat(stringNonce);
    		//FIXME System.out.println(toDigest);

    		 String toSend=makeDigitalSignature(toDigest,myName);
    		if(toSend==null){
    			System.out.println("Error while signing");
    			return false;
    		}
    		//add resume to the header
    		name = se.createName("resume","d","http://demo");
    		element = sh.addHeaderElement(name);
    		element.addTextNode(toSend);
    		//element.addTextNode(toDigest);
    	}catch(Exception e){System.out.println(e);}

    	//System.out.println("exiting handler");
    	//System.out.println("");
    	return true;
    }







    public boolean handleResponse(MessageContext context){
    	//System.out.println(this + ">\n\t handleResponse(MessageContext=" + context+ ")");
    	try {
    		//System.out.println("handling Response");

    		SOAPMessageContext smc = (SOAPMessageContext) context;
    		SOAPMessage msg = smc.getMessage();
    		SOAPPart sp = msg.getSOAPPart();
    		SOAPEnvelope se = sp.getEnvelope();
    		SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
    		if (sh == null) {sh = se.addHeader();}
    		//TODO verificar este namespace

    		//take senders name
    		Name sendersName = se.createName("senderName","d","http://demo");
    		Iterator it =sh.getChildElements(sendersName);
    		if (!it.hasNext()) {
                System.out.println("Sender header element not found.");
                makeFault(sb,"missingSender","Sender header element not found.");
                return false;
            }
            SOAPElement senderelement = (SOAPElement) it.next();
    		String senderName = senderelement.getValue();
    		//System.out.println("send by: "+senderName);

    		//take destination name
    		Name destinationname = se.createName("destinationName","d","http://demo");
    		it =sh.getChildElements(destinationname);
    		if (!it.hasNext()) {
                System.out.println("Destination header element not found.");
                makeFault(sb,"missingDestination","Destination header element not found.");
                return false;
            }
            SOAPElement destinationElement = (SOAPElement) it.next();
    		String destinationName = destinationElement.getValue();
    		//System.out.println("message destination: "+destinationName);

    		//take nonce
    		Name noncName = se.createName("nonce","d","http://demo");
    		it =sh.getChildElements(noncName);
    		if (!it.hasNext()) {
                System.out.println("Nonce header element not found.");
                makeFault(sb,"missingNonce","Nonce header element not found.");
                return false;
            }
    		SOAPElement nonceElement = (SOAPElement) it.next();
    		String nonceValue = nonceElement.getValue();
    		nonceReceived = Integer.parseInt(nonceValue);
    		//System.out.println("nonce received: "+nonceValue);
    		if(!checkNonce(nonceReceived)){
    			System.out.println("Nonce not the expected");
    			makeFault(sb,"wrongNonce","Nonce not the expected");
    			return false;
    		}
    		//take resume
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
    		toDigest= toDigest.concat(senderName);
    		toDigest= toDigest.concat(destinationName);
    		toDigest= toDigest.concat(nonceValue);
    		//System.out.println(toDigest);

    		boolean signatureAutentic ;
    		if(myName==null||othersName==null){
    			System.out.println("server first time");
    			signatureAutentic = checkSignature(senderName,destinationName,toDigest,signedResume);
    			if(!signatureAutentic){
    				System.out.println("Violated message: The signature check FAILED !!!!!!");
    				makeFault(sb,"ViolatedMessage","The signature check FAILED");
    				return false;
    			}
    			firstReceivedOriginName=senderName;//used to check if this received parameters are the expected
    			firstReceivedDestinationName=destinationName;
    			needToCheckNames=true;
    		}
    		else{
    			if(!myName.equals(destinationName)||!othersName.equals(senderName)){
    				System.out.println("!!! Received Message from a not expecting sender or with wrong destination !!!");
    				makeFault(sb,"WrongEndPoints","!!! Received Message from a not expecting sender or with wrong destination !!!");
    				return false;
    			}
    			signatureAutentic = checkSignature(othersName,myName,toDigest,signedResume);
    			if(!signatureAutentic){
    				System.out.println("!!!The signature isnt ok");
    				makeFault(sb,"ViolatedMessage","The signature check FAILED");
    			}
    		}

    		sh.detachNode();
    		//System.out.println("");
    		return signatureAutentic;
    		/*sb.removeContents();
    		SOAPFault fault = sb.addFault();
    		QName faultName = new QName("SignatureIncorrect");
    		fault.setFaultCode(faultName);
    		fault.setFaultString("Message  received does not have a correct signature");*/

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
    	//System.out.println("START CHEKING SIGNATURE");
    	//System.out.println("othername: "+ otherName+ " myname: "+myName);
    	final byte[] plainBytesText = parseBase64Binary(text);
    	final byte[] plainBytesSignedText = parseBase64Binary(signedText);

    	String myKeyStore = myName.concat(".jks");

		String current=null;
		try {current = new java.io.File( "." ).getCanonicalPath();
		} catch (IOException e1) {e1.printStackTrace();}

		KeyStore ks=null;
		try {ks = KeyStore.getInstance("jks");
		} catch (KeyStoreException e1) {//e1.printStackTrace();
			}

		//System.out.println("loading key store");
		char[] passwordFile = "ins3cur3".toCharArray();
		String path=current + "/../keys/" + myName + "/" + myKeyStore;
		try {ks.load(new FileInputStream(current + "/../keys/" + myName + "/" + myKeyStore), passwordFile);
		} catch (NoSuchAlgorithmException | CertificateException | IOException e1) {e1.printStackTrace();}

		//System.out.println("geting x509certeficate");

		X509Certificate certificate=null;
		try {certificate = (X509Certificate) ks.getCertificate(otherName.toLowerCase());
		} catch (KeyStoreException e1) {e1.printStackTrace();}
		if(certificate==null){
			System.out.println("Does not have certificate of: "+ otherName);
			String uddiURL = "http://localhost:9090";
			try {
				CAClient cac = new CAClient(uddiURL,"CertAuth");
				certificate=cac.saveCertificate(otherName.toLowerCase(),path,passwordFile);
			} catch (Exception e) {	e.printStackTrace();}
			if (certificate==null){
				System.out.println("Entity not recognized by CA");
				return false;
			}
		}

		//PublicKey pk = certificate.getPublicKey();

		//System.out.println(" verify the signature with the public key");
        try {
        	Signature sig = Signature.getInstance("SHA1WithRSA");
        	//sig.initVerify(pk);
        	sig.initVerify(certificate);
        	sig.update(plainBytesText);
        	//System.out.println("signature check");
        	//System.out.println("END CHEKING SIGNATURE1");
            return sig.verify(plainBytesSignedText);
        } catch (SignatureException se) {
        	System.out.println("Exception veryfying certeficate"+se);
            return false;
        } catch (Exception e){ System.out.println("Exception veryfying certeficate"+e);
        System.out.println("Exception veryfying certeficate"+ e);}
        return false;
    }


    public static String makeDigitalSignature(String text,String myName) throws Exception {
    	//System.out.println("START MAKING SIGNATURE");
    	//System.out.println(" myname: "+myName);
    	byte[] plainBytesText;
    	try{
    	plainBytesText = parseBase64Binary(text);
    	}catch(Exception e){
    		System.out.println("erro a passar para bytes"+e);
    		return null;
    	}
    	//System.out.println("Get the others public key");
    	String myKeyStore = myName.concat(".jks");
		String current = new java.io.File( "." ).getCanonicalPath();
		KeyStore ks  = KeyStore.getInstance("jks");
		char[] password = "ins3cur3".toCharArray();
		ks.load(new FileInputStream(current + "/../keys/" + myName + "/" + myKeyStore), password);
		password = "1nsecure".toCharArray();
		Key privateKey =(PrivateKey) ks.getKey(myName.toLowerCase(),password);
		//System.out.println("get a signature object using the MD5 and RSA combo");
    	// and sign the plaintext with the private key
    	Signature sig = Signature.getInstance("SHA1WithRSA");
    	sig.initSign((PrivateKey) privateKey);
    	sig.update(plainBytesText);
    	byte[] signature = sig.sign();

    	//System.out.println("return the signature");
    	return printBase64Binary(signature);
    }

    public boolean checkNonce(int nonceReceived){
    	return nonceReceived == nonce;

    }

    public int getNonce(){
    	nonce =randomGenerator.nextInt(1000000000);
    	return nonce;
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
        	if(faultCode.equals("missingSender")||
        			faultCode.equals("missingDestination")||
        			faultCode.equals("missingNonce")||
        			faultCode.equals("wrongNonce")||
        			faultCode.equals("missingResume")||
        			faultCode.equals("ViolatedMessage")||
        			faultCode.equals("WrongEndPoints")
        			)
        	throw new SOAPFaultException(sb.getFault());
        	//throw new RuntimeException();
        }
        return true;
    }

    public void close(MessageContext messageContext) {
    }

/*		QName faultName = new QName(faultNamein);
		fault.setFaultCode(faultName);
		fault.setFaultString(faultText);*/





}
