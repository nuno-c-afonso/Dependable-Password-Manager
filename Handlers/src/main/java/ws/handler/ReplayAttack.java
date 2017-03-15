package ws.handler;

import java.util.Iterator;
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
public class ReplayAttack implements SOAPHandler<SOAPMessageContext> {

    public static final String CONTEXT_PROPERTY = "my.property";

    int n=0;
    SOAPMessage putMessageToRepeat=null;
    SOAPHeader putShToRepeat=null;
    SOAPMessage getMessageToRepeat=null;
    SOAPHeader getShToRepeat=null;
    
    public Set<QName> getHeaders() {
        return null;
    }

    public boolean handleMessage(SOAPMessageContext smc) {

    
        Boolean outboundElement = (Boolean) smc
                .get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        	

        try {
            if (outboundElement.booleanValue()) {
        		SOAPMessage msg = smc.getMessage();
        		SOAPPart sp = msg.getSOAPPart();
        		SOAPEnvelope se = sp.getEnvelope();
        		SOAPBody sb = se.getBody();
        		SOAPHeader sh = se.getHeader();
        		//requestJobResponse
        		
        		//ns2:put xmlns:ns2="http://server.dpm.sec.tecnico.pt/
        		
        		
        		
        		Name RegisterRequeste = se.createName("register","ns2","http://server.dpm.sec.tecnico.pt/");
        		Iterator it =sb.getChildElements(RegisterRequeste);
        		if (it.hasNext()) {
        			System.out.println("\n Register message do nothing\n");
        			return true;
                }
        		
        		SOAPFault fault =sb.getFault();


            	
            	n++;
      		 		
     		
        		Name putRequeste = se.createName("put","ns2","http://server.dpm.sec.tecnico.pt/");
        		it =sb.getChildElements(putRequeste);
        		if (it.hasNext()) {
                	if((putMessageToRepeat==null || n==2)&& fault==null){
                		System.out.println("\n 1a Vez que recebe a mensagem put\n");
            			putMessageToRepeat=msg;
            			putShToRepeat=sh;
            			n=0;
            			return true;
            		}
                	
                	System.out.println("start repeating");
           			SOAPPart sp1 = putMessageToRepeat.getSOAPPart();
            		SOAPEnvelope se1 = sp1.getEnvelope();
            		if(se1.getHeader()==null){
            			System.out.println("\n3\n");
            			SOAPHeader sh1 = se1.addHeader();
            			Iterator it1 =putShToRepeat.getChildElements();
            			while(it1.hasNext())
            				sh1.appendChild((SOAPElement) it1.next());        			 
            		}
            		
            		smc.setMessage(putMessageToRepeat);
            		msg=smc.getMessage();
       			
        			
        			return true;
                }
        		
        		Name getRequest = se.createName("get","ns2","http://server.dpm.sec.tecnico.pt/");
        		it =sb.getChildElements(getRequest);
        		if (it.hasNext()) {
                	if((getMessageToRepeat==null || n==2)&& fault==null){
                		System.out.println("\n 1a Vez que recebe a mensagem get\n");
            			getMessageToRepeat=msg;
            			getShToRepeat=sh;
            			n=0;
            			return true;
            		}
        			
                	
                	System.out.println("start repeating get");
           			SOAPPart sp1 = getMessageToRepeat.getSOAPPart();
            		SOAPEnvelope se1 = sp1.getEnvelope();
            		if(se1.getHeader()==null){
            			System.out.println("\n3\n");
            			SOAPHeader sh1 = se1.addHeader();
            			Iterator it1 =getShToRepeat.getChildElements();
            			while(it1.hasNext())
            				sh1.appendChild((SOAPElement) it1.next());        			 
            		}
            		
            		smc.setMessage(getMessageToRepeat);
            		msg=smc.getMessage();
        			
                    return true;
        		}

            	
           /* 	
        		System.out.println("start repeating");
       			SOAPPart sp1 = putMessageToRepeat.getSOAPPart();
        		SOAPEnvelope se1 = sp1.getEnvelope();
        		if(se1.getHeader()==null){
        			System.out.println("\n3\n");
        			SOAPHeader sh1 = se1.addHeader();
        			Iterator it1 =putShToRepeat.getChildElements();
        			while(it1.hasNext())
        				sh1.appendChild((SOAPElement) it1.next());        			 
        		}
        		
            		
        		
        		System.out.println(msg.getSOAPPart().getEnvelope().getBody().getTextContent());
        		smc.setMessage(putMessageToRepeat);
        		msg=smc.getMessage();
        		System.out.println(msg.getSOAPPart().getEnvelope().getBody().getTextContent());
        		System.out.println("message repeted");
        		System.out.println("\n");
        		return true;*/
        		
            }
            return true;
        } catch (Exception e) {}
        return true;
    }

    public boolean handleFault(SOAPMessageContext smc) {
       /* System.out.println("handling fault on Replay Attack handler");
        SOAPBody sb=null;
        SOAPFault fault;
        try{
	        SOAPMessage msg = smc.getMessage();
			SOAPPart sp = msg.getSOAPPart();
			SOAPEnvelope se = sp.getEnvelope();
			sb = se.getBody();
        }catch(Exception e){}
        if(sb!=null){
        	throw new SOAPFaultException(sb.getFault());
        }*/
        return true;
    }

    public void close(MessageContext messageContext) {
    }

}