package ws.handler;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class ChangePubKeyHandler implements SOAPHandler<SOAPMessageContext>{
	
	public static final String OTHERSNAME= "my.othersname.property";
	
	PublicKey pubKey = null;
	
	PrivateKey privKey = null;
	
	String registerPacket;
	Boolean register = false;
	Boolean firstTime = true;
	
	
	@Override
	public void close(MessageContext arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean handleFault(SOAPMessageContext arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean handleMessage(SOAPMessageContext context){
        Boolean outboundElement = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		 if(outboundElement.booleanValue()) {
			 return handleRequest(context);
		 }
		 else
			 return handleResponse(context);
		 
	}
	
	public boolean handleRequest(MessageContext context) {
		if(privKey == null) {
			setUp();
			registerPacket = "<S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
					+ "<SOAP-ENV:Header/>"
					+ "<S:Body><ns2:register xmlns:ns2=\"http://server.dpm.sec.tecnico.pt/\">"
					+ "<arg0>"
					+ Base64.getEncoder().encodeToString(pubKey.getEncoded())
					+ "</arg0>"
					+ "</ns2:register>"
					+ "</S:Body>"
					+ "</S:Envelope>";
		}
		try {
			
			SOAPMessageContext smc = (SOAPMessageContext) context;
			SOAPMessage msg = smc.getMessage();
			SOAPPart sp = msg.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
			
			
			Name name = se.createName("put", "ns2", "http://server.dpm.sec.tecnico.pt/");
			Iterator it = sb.getChildElements(name);
			Name nameRegister = se.createName("register", "ns2", "http://server.dpm.sec.tecnico.pt/");
			Iterator itReg = sb.getChildElements(nameRegister);
			if(itReg.hasNext() && firstTime ){
				firstTime = false;
			} else if(itReg.hasNext() && !firstTime ){
				register = true;
			}
			if(it.hasNext() || (itReg.hasNext() && register)) {		
				
				
				SOAPElement method = (SOAPElement) sb.getFirstChild();
				Name key = se.createName("arg0");
				Iterator itAUX = method.getChildElements(key);
				if(!itAUX.hasNext()) {
					System.out.println("Nao encontra key");
				
					return true;
				}
				
				
				SOAPElement pubkey = (SOAPElement) itAUX.next();
				
				String strKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
				
				pubkey.setValue(strKey);
				
				Name resumeName = se.createName("resume","d","http://demo");
				Iterator itResume = sh.getChildElements(resumeName);
				if(!itResume.hasNext()) {
					System.out.println("Nao encontra resume");
					return true;
				}
				
				SOAPElement resume = (SOAPElement) itResume.next();
				
				String stringTimestamp = Long.toString(System.currentTimeMillis());
				String othersName = ((String) context.get(OTHERSNAME)).toLowerCase().replace('/','0');
				
				Name timeName = se.createName("timeStamp","d","http://demo");
	    		it = sh.getChildElements(timeName);
	    		if (!it.hasNext()) {
	                System.out.println("timeStamp header element not found.");
	                return true;
	            }
				
				SOAPElement timestamp = (SOAPElement) it.next();
				timestamp.setValue(stringTimestamp);
				
				//generate new signature

				String toDigest = sb.getTextContent();
	    		toDigest= toDigest.concat(othersName);
	    		toDigest= toDigest.concat(stringTimestamp);
	    		
				resume.setValue(makeDigitalSignature(toDigest));
					
			}
			
		} catch (SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	// Bypass to the top one
    public boolean handleResponse(MessageContext context){
    	return true;
    }
        
        

	@Override
	public Set<QName> getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	public void setUp() {
		KeyPairGenerator keyGen;
		try {
			keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.genKeyPair();
			pubKey = keyPair.getPublic();
			privKey = keyPair.getPrivate();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
    public String makeDigitalSignature(String text) throws Exception {

    	byte[] plainBytesText;
    	try{
    		plainBytesText = parseBase64Binary(text);
    	}catch(Exception e){
    		System.out.println("erro a passar para bytes"+e);
    		return null;
    	}
    	      
        
    	Signature sig = Signature.getInstance("SHA256WithRSA");
    	sig.initSign(privKey);
    	sig.update(plainBytesText);
    	byte[] signature = sig.sign();

    	return printBase64Binary(signature);
    }

}
