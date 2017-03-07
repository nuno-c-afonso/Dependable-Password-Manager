package example.ws.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Iterator;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.StrictExpectations;
import mockit.integration.junit4.JMockit;


/**
 *  Handler test suite
 */

@RunWith(JMockit.class)
public class SignatureHandlerTest extends AbstractHandlerTest {
	public static KeyStore ks = null;

	@Before
    public void beforeTest() {
		String current = null;

		try {
			current = new java.io.File( "." ).getCanonicalPath();
			ks = KeyStore.getInstance("jks");
			ks.load(new FileInputStream(current + "/src/test/resources/Broker.jks"), "ins3cur3".toCharArray());
		} catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
			e.printStackTrace();
		}

		new MockUp<KeyStore>() {
    		@Mock
    		KeyStore getInstance(String s) {
    			return ks;
    		}

    		@Mock
    		void load(InputStream s, char[] p) { }
    	};
    }

    // tests

    @Test
    public void testSignatureHandlerOutbound(
        @Mocked final SOAPMessageContext soapMessageContext)
        throws Exception {

    	new MockUp<SignatureHandler>() {
    		@Mock
    		int getNonce () {
    	    	return NONCE;
    	    }
    	};

        final String soapText = BROKER_SOAP_REQUEST_BEFORE;
        // System.out.println(soapText);

        final SOAPMessage soapMessage = byteArrayToSOAPMessage(soapText.getBytes());
        final Boolean soapOutbound = true;

        new StrictExpectations() {{
            soapMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            result = soapOutbound;

            soapMessageContext.get(MYNAME);
            result = "Broker";

            soapMessageContext.get(OTHERSNAME);
            result = "UpaTransporter1";

            soapMessageContext.getMessage();
            result = soapMessage;
        }};

        SignatureHandler handler = new SignatureHandler();
        boolean handleResult = handler.handleMessage(soapMessageContext);

        // assert that message would proceed normally
        assertTrue(handleResult);

        // assert header
        SOAPPart soapPart = soapMessage.getSOAPPart();
        SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
        SOAPHeader soapHeader = soapEnvelope.getHeader();
        assertNotNull(soapHeader);

        // assert header element
        Name name = soapEnvelope.createName("senderName", "d", "http://demo");
        Iterator it = soapHeader.getChildElements(name);
        assertTrue(it.hasNext());

        // assert header element value
        SOAPElement element = (SOAPElement) it.next();
        String valueString = element.getValue();
        assertEquals("Broker", valueString);

        // assert header element
        name = soapEnvelope.createName("destinationName", "d", "http://demo");
        it = soapHeader.getChildElements(name);
        assertTrue(it.hasNext());

        // assert header element value
        element = (SOAPElement) it.next();
        valueString = element.getValue();
        assertEquals("UpaTransporter1", valueString);

        // assert header element
        name = soapEnvelope.createName("nonce", "d", "http://demo");
        it = soapHeader.getChildElements(name);
        assertTrue(it.hasNext());

        // assert header element value
        element = (SOAPElement) it.next();
        String nonce = element.getValue();
        assertEquals("The nonces should be the same", Integer.toString(NONCE), nonce);

        // assert header element
        name = soapEnvelope.createName("resume", "d", "http://demo");
        it = soapHeader.getChildElements(name);
        assertTrue(it.hasNext());

        // assert header element value
        element = (SOAPElement) it.next();
        valueString = element.getValue();
        assertEquals("The value should be the same.", BROKER_SIGNATURE, valueString);
    }

    @Test
    public void testSignatureHandlerInbound(
    	@Mocked final SOAPMessageContext soapMessageContext)
        throws Exception {

    	new MockUp<SignatureHandler>() {
    		@Mock
    		boolean checkNonce (int nonceReceived) {
    	    	return nonceReceived == NONCE;
    	    }
    	};

    	final String soapText = TRANSPORTER_SOAP_RESPONSE_AFTER;
        // System.out.println(soapText);

        final SOAPMessage soapMessage = byteArrayToSOAPMessage(soapText.getBytes());
        final Boolean soapOutbound = false;

        new StrictExpectations() {{
        	soapMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            result = soapOutbound;

            soapMessageContext.getMessage();
            result = soapMessage;
        }};

        SignatureHandler handler = new SignatureHandler();
        boolean handleResult = handler.handleMessage(soapMessageContext);

        // assert that message would proceed normally
        assertTrue(handleResult);
    }

    @Test
    public void TestChangeParameter(
    	@Mocked final SOAPMessageContext soapMessageContext)
        throws Exception {

    	new MockUp<SignatureHandler>() {
    		@Mock
    		boolean checkNonce (int nonceReceived) {
    	    	return nonceReceived == NONCE;
    	    }
    	};

    	final String soapText = TRANSPORTER_SOAP_RESPONSE_AFTER_ADULTERED;
        // System.out.println(soapText);

        final SOAPMessage soapMessage = byteArrayToSOAPMessage(soapText.getBytes());
        final Boolean soapOutbound = false;

        new StrictExpectations() {{
        	soapMessageContext.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
            result = soapOutbound;

            soapMessageContext.getMessage();
            result = soapMessage;
        }};

        SignatureHandler handler = new SignatureHandler();
        boolean handleResult = handler.handleMessage(soapMessageContext);

        // assert that message would proceed normally
        assertFalse(handleResult);
    }


}
