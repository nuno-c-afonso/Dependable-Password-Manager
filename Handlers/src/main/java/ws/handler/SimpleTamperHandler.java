package ws.handler;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class SimpleTamperHandler implements SOAPHandler<SOAPMessageContext> {
	public static final String ATTACKER_KEY = "MIICITANBgkqhkiG9w0BAQEFAAOCAg4AMIICCQKCAgBupaNSzNq/GM+gLI7CtM81" +
											  "x2XyklMO4QvqMb57CLxNj/Ah6Y69Mkmig4bWDZVdRpd1+0WVX2RSLkDbWFRVECMj" +
											  "Njcq+i/NRj1mEPDR0CWpnlGkNh9Urr4ssjmHP7AKw5ujb21waUArDO1/mePTFTJN" +
											  "ii0agUzDA/EHfLoNlD2Q3oouMlEP3pUhHPerc8xnBkg1t41gLNNEpmldsUvji35L" +
											  "lAr7NMMtaLRg5G9p7v2bfFuy7XlVDakJHPrIp69af5965KmnW9HBhvFUYTGqlagi" +
											  "hqp7/E/I+GKZ1TA+iKALYJSBgyZdQzx0ZBT6WtwgJEnJpVghxdcUqyBznCFJuqaH" +
											  "XtRiCLfef+LrxNnDRywMsTzV+bV3KIBckk0F6d1ijlZuj2Ln9qZJ8FapPlcGONvM" +
											  "DLzAS42rTiNkzvNg5TLwngyKD7arfX9610PF1t0AMS9igCLIKV6x6uKlGv5C7OZK" +
											  "EWCyzcKr+GHi4AwLUrt1rhRD1wqZokqpgldDvAf0Tekud1mkV3dBdrEExZMhXIBG" +
											  "fF3t2GaD8p/ma1VsJQqCxO/HAJmaBhfGgd2X4lp1EubQtZWonplFi3zIDZv5p2ST" +
											  "AWdIlSd1dsoMfp9mv+kECW5dDhQQi7u8JEHO1Rk9o0b0NyAVIQ0IiEfHkhcG8jvy" +
											  "WlnVA7mJND25vAYzjEe4/QIDAQAB";
	
	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		Boolean outboundElement = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
		 if(outboundElement.booleanValue()) {
			 return handleRequest(context);
		 }
		 else
			 return handleResponse(context);
	}

	private boolean handleResponse(SOAPMessageContext context) {
		// Passes to the upper handler
		return true;
	}

	private boolean handleRequest(SOAPMessageContext context) {
    	try {
    		
    		//Dissecate the message
    		SOAPMessageContext smc = (SOAPMessageContext) context;
    		SOAPMessage msg = smc.getMessage();
    		SOAPPart sp = msg.getSOAPPart();
    		SOAPEnvelope se = sp.getEnvelope();
    		SOAPBody sb = se.getBody();
    		SOAPHeader sh = se.getHeader();
    		
    		SOAPElement method =(SOAPElement) sb.getFirstChild();
    		Name key = se.createName("arg0");
            Iterator it =method.getChildElements(key);
            if (!it.hasNext()) {
            	System.out.println("nao encontra o key");
                return false;
            } 
            
            //get the client PUBLIC KEY from the message
            SOAPElement pubkey = (SOAPElement) it.next();
            pubkey.setValue(ATTACKER_KEY);
    		
    	} catch(Exception e){
    		System.out.println(e);
    	}
    	
    	return true;
	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		return true;
	}

	@Override
	public void close(MessageContext context) {
		// Empty
	}

	@Override
	public Set<QName> getHeaders() {
		return null;
    }
}
