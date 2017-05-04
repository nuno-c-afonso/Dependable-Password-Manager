package ws.handler;

import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.MessageContext.Scope;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;

/**
 *  This SOAPHandler shows how to set/get values from headers in
 *  inbound/outbound SOAP messages.
 *
 *  A header is created in an outbound message and is read on an
 *  inbound message.
 *
 *  The value that is read from the header
 *  is placed in a SOAP message context property
 *  that can be accessed by other handlers or by the application.
 */
public class DelayMessagesAttack implements SOAPHandler<SOAPMessageContext> {

    public static final String CONTEXT_PROPERTY = "my.property";
    Random rn = new Random();
    
    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleMessage(SOAPMessageContext smc) {
        	
    	int i = Math.abs(rn.nextInt()) % 5000;
    	try {
			Thread.sleep(i);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
        return true;
    }

    public boolean handleFault(SOAPMessageContext smc) {

        return true;
    }

    public void close(MessageContext messageContext) {
    }

}