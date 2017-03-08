package ws.handler;

import java.io.ByteArrayInputStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 *  Abstract handler test suite
 */
public abstract class AbstractHandlerTest {

    // static members

    /** hello-ws SOAP request message captured with LoggingHandler */
    protected static final String HELLO_SOAP_REQUEST = "<S:Envelope " +
    "xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
    "<SOAP-ENV:Header/>" +
    "<S:Body>" +
    "<ns2:sayHello xmlns:ns2=\"http://ws.example/\">" +
    "<arg0>friend</arg0>" +
    "</ns2:sayHello>" +
    "</S:Body></S:Envelope>";

    /** hello-ws SOAP response message captured with LoggingHandler */
    protected static final String HELLO_SOAP_RESPONSE = "<S:Envelope " +
    "xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
    "<SOAP-ENV:Header/>" +
    "<S:Body>" +
    "<ns2:sayHelloResponse xmlns:ns2=\"http://ws.example/\">" +
    "<return>Hello friend!</return>" +
    "</ns2:sayHelloResponse>" +
    "</S:Body></S:Envelope>";
    
    /** broker-ws SOAP request message before signing captured with LoggingHandler */
    protected static final String BROKER_SOAP_REQUEST_BEFORE = "<S:Envelope xmlns:" + 
    "S=\"http://schemas.xmlsoap.org/soap/envelope/\" " + 
    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">" + 
    "<SOAP-ENV:Header/><S:Body><ns2:requestJob xmlns:ns2=\"http://ws.transporter.upa.pt/\">" +
    "<origin>Lisboa</origin><destination>Beja</destination><price>9</price></ns2:requestJob>" +
    "</S:Body></S:Envelope>";

    /** broker-ws SOAP request message after signing captured with LoggingHandler */
    protected static final String BROKER_SOAP_REQUEST_AFTER = "<S:Envelope " + 
    "xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" " + 
    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header>" + 
    "<d:senderName xmlns:d=\"http://demo\">Broker</d:senderName>" + 
    "<d:destinationName xmlns:d=\"http://demo\">UpaTransporter1</d:destinationName>" + 
    "<d:nonce xmlns:d=\"http://demo\">436656</d:nonce>" + 
    "<d:resume xmlns:d=\"http://demo\">TIKITsDcCvEM0PG61lcd4PrF1E7Yb+HlEh489PyLMM6N7"+
    "wXVR21V4ARHQzPJ/pKnQI32VGZ8+EGbq1dmnhAcJXNjOnjZxy0TtRDuaQKtyIdxWPrBd5h6mRdAFyW" + 
    "tifJ06C2yzwhk8Jrpfaek+YaEleC5FxNOruplyV5T/gLTPWYbrYsIkvbPcYTnlEtC0TVF3qHUMpyDh" +
    "qc2JlUIwnFfr0eYHjVgBVBpsQ0tV79SxGhVnA3Vf3kDxeT9akeStsuyPjnjRh0Wf5y5MDCthLcD5pm" +
    "oZInHgl1GG8unyQy2kloGDh7jlVbKddC4VQ73HE/OjnCvSE9PkJxcnvUq4rzUjw==</d:resume>" +
    "</SOAP-ENV:Header><S:Body><ns2:requestJob xmlns:ns2=\"http://ws.transporter.upa.pt/\">" +
    "<origin>Lisboa</origin><destination>Beja</destination><price>9</price></ns2:requestJob>" +
    "</S:Body></S:Envelope>";
    
    protected static final String BROKER_SOAP_REQUEST_AFTER_ADULTERED = "<S:Envelope " + 
    	    "xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" " + 
    	    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header>" + 
    	    "<d:senderName xmlns:d=\"http://demo\">Broker</d:senderName>" + 
    	    "<d:destinationName xmlns:d=\"http://demo\">UpaTransporter1</d:destinationName>" + 
    	    "<d:nonce xmlns:d=\"http://demo\">10000</d:nonce>" + 
    	    "<d:resume xmlns:d=\"http://demo\">TIKITsDcCvEM0PG61lcd4PrF1E7Yb+HlEh489PyLMM6N7"+
    	    "wXVR21V4ARHQzPJ/pKnQI32VGZ8+EGbq1dmnhAcJXNjOnjZxy0TtRDuaQKtyIdxWPrBd5h6mRdAFyW" + 
    	    "tifJ06C2yzwhk8Jrpfaek+YaEleC5FxNOruplyV5T/gLTPWYbrYsIkvbPcYTnlEtC0TVF3qHUMpyDh" +
    	    "qc2JlUIwnFfr0eYHjVgBVBpsQ0tV79SxGhVnA3Vf3kDxeT9akeStsuyPjnjRh0Wf5y5MDCthLcD5pm" +
    	    "oZInHgl1GG8unyQy2kloGDh7jlVbKddC4VQ73HE/OjnCvSE9PkJxcnvUq4rzUjw==</d:resume>" +
    	    "</SOAP-ENV:Header><S:Body><ns2:requestJob xmlns:ns2=\"http://ws.transporter.upa.pt/\">" +
    	    "<origin>Lisboa</origin><destination>Beja</destination><price>9</price></ns2:requestJob>" +
    	    "</S:Body></S:Envelope>";
    
    protected static final String BROKER_SIGNATURE = "TIKITsDcCvEM0PG61lcd4PrF1E7Yb+HlEh489PyLMM6N7"+
    	    "wXVR21V4ARHQzPJ/pKnQI32VGZ8+EGbq1dmnhAcJXNjOnjZxy0TtRDuaQKtyIdxWPrBd5h6mRdAFyW" + 
    	    "tifJ06C2yzwhk8Jrpfaek+YaEleC5FxNOruplyV5T/gLTPWYbrYsIkvbPcYTnlEtC0TVF3qHUMpyDh" +
    	    "qc2JlUIwnFfr0eYHjVgBVBpsQ0tV79SxGhVnA3Vf3kDxeT9akeStsuyPjnjRh0Wf5y5MDCthLcD5pm" +
    	    "oZInHgl1GG8unyQy2kloGDh7jlVbKddC4VQ73HE/OjnCvSE9PkJxcnvUq4rzUjw==";
    
    protected static final int NONCE = 436656;

    /** transporter-ws SOAP response message before signing captured with LoggingHandler */
    protected static final String TRANSPORTER_SOAP_RESPONSE_BEFORE = "<S:Envelope " + 
    "xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" " + 
    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header/>" +
    "<S:Body><ns2:requestJobResponse xmlns:ns2=\"http://ws.transporter.upa.pt/\">" + 
    "<return><companyName>UpaTransporter1</companyName>" + 
    "<jobIdentifier>5e693d32-f4ca-4b2b-8527-8ee68afa442e</jobIdentifier>" +
    "<jobOrigin>Lisboa</jobOrigin><jobDestination>Beja</jobDestination>" + 
    "<jobPrice>8</jobPrice><jobState>PROPOSED</jobState></return>" + 
    "</ns2:requestJobResponse></S:Body></S:Envelope>";

    /** transporter-ws SOAP response message after signing captured with LoggingHandler */
    protected static final String TRANSPORTER_SOAP_RESPONSE_AFTER = "<S:Envelope " + 
    "xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" " + 
    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header>" + 
    "<d:senderName xmlns:d=\"http://demo\">UpaTransporter1</d:senderName>" + 
    "<d:destinationName xmlns:d=\"http://demo\">Broker</d:destinationName>" + 
    "<d:nonce xmlns:d=\"http://demo\">436656</d:nonce>" + 
    "<d:resume xmlns:d=\"http://demo\">UsOjlXyVngX6atIkeZrXdEmi3xplbHjcD5BIyJmojsgcQd" + 
    "duowuTepGUQSzCs06JEBLPbydARs5To9bQbEwxFqfdY33mMH+1yRVK/rlLRFm0wj1dSqBCdORYmUILWf8" +
    "f5mbMkQ8GViWP9ah9h20viKgBX5VCKtLYFM9UZl7iSily4leVBlNi41xKleQajjHmcELMZK042H2nTWiY" + 
    "hnFnPik7z9kPjSYgKAQbwfedO/THJ49cdmdy9lbv/VdXkthZpnkG2vTpkXOa8/LefWDyxbzbtDmbnxUO" +
    "7I/Lxfu/1Ul4Gtaz5LrzMv1w1AcAyxn7EsUvLOqGVMucEA30vnIQcg==</d:resume>" + 
    "</SOAP-ENV:Header><S:Body>" + 
    "<ns2:requestJobResponse xmlns:ns2=\"http://ws.transporter.upa.pt/\"><return>" +
    "<companyName>UpaTransporter1</companyName>" + 
    "<jobIdentifier>5e693d32-f4ca-4b2b-8527-8ee68afa442e</jobIdentifier>" +
    "<jobOrigin>Lisboa</jobOrigin><jobDestination>Beja</jobDestination><jobPrice>8</jobPrice>" +
    "<jobState>PROPOSED</jobState></return></ns2:requestJobResponse></S:Body></S:Envelope>";
    
    protected static final String TRANSPORTER_SOAP_RESPONSE_AFTER_ADULTERED = "<S:Envelope " + 
    	    "xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\" " + 
    	    "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP-ENV:Header>" + 
    	    "<d:senderName xmlns:d=\"http://demo\">UpaTransporter1</d:senderName>" + 
    	    "<d:destinationName xmlns:d=\"http://demo\">Broker</d:destinationName>" + 
    	    "<d:nonce xmlns:d=\"http://demo\">436656</d:nonce>" + 
    	    "<d:resume xmlns:d=\"http://demo\">UsOjlXyVngX6atIkeZrXdEmi3xplbHjcD5BIyJmojsgcQd" + 
    	    "duowuTepGUQSzCs06JEBLPbydARs5To9bQbEwxFqfdY33mMH+1yRVK/rlLRFm0wj1dSqBCdORYmUILWf8" +
    	    "f5mbMkQ8GViWP9ah9h20viKgBX5VCKtLYFM9UZl7iSily4leVBlNi41xKleQajjHmcELMZK042H2nTWiY" + 
    	    "hnFnPik7z9kPjSYgKAQbwfedO/THJ49cdmdy9lbv/VdXkthZpnkG2vTpkXOa8/LefWDyxbzbtDmbnxUO" +
    	    "7I/Lxfu/1Ul4Gtaz5LrzMv1w1AcAyxn7EsUvLOqGVMucEA30vnIQcg==</d:resume>" + 
    	    "</SOAP-ENV:Header><S:Body>" + 
    	    "<ns2:requestJobResponse xmlns:ns2=\"http://ws.transporter.upa.pt/\"><return>" +
    	    "<companyName>UpaTransporter1</companyName>" + 
    	    "<jobIdentifier>5e693d32-f4ca-4b2b-8527-8ee68afa442e</jobIdentifier>" +
    	    "<jobOrigin>Lisboa</jobOrigin><jobDestination>Beja</jobDestination><jobPrice>100</jobPrice>" +
    	    "<jobState>PROPOSED</jobState></return></ns2:requestJobResponse></S:Body></S:Envelope>";
    
    protected static final String TRANSPORTER_SIGNATURE = "UsOjlXyVngX6atIkeZrXdEmi3xplbHjcD5BIyJmojsgcQd" + 
    "duowuTepGUQSzCs06JEBLPbydARs5To9bQbEwxFqfdY33mMH+1yRVK/rlLRFm0wj1dSqBCdORYmUILWf8" +
    "f5mbMkQ8GViWP9ah9h20viKgBX5VCKtLYFM9UZl7iSily4leVBlNi41xKleQajjHmcELMZK042H2nTWiY" + 
    "hnFnPik7z9kPjSYgKAQbwfedO/THJ49cdmdy9lbv/VdXkthZpnkG2vTpkXOa8/LefWDyxbzbtDmbnxUO" +
    "7I/Lxfu/1Ul4Gtaz5LrzMv1w1AcAyxn7EsUvLOqGVMucEA30vnIQcg==";

    /** SOAP message factory */
    protected static final MessageFactory MESSAGE_FACTORY;
    public static final String MYNAME= "my.myname.property";
    public static final String OTHERSNAME= "my.othersname.property";

    static {
        try {
            MESSAGE_FACTORY = MessageFactory.newInstance();
        } catch(SOAPException e) {
            throw new RuntimeException(e);
        }
    }


    // helper functions

    protected static SOAPMessage byteArrayToSOAPMessage(byte[] msg) throws Exception {
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(msg);
        StreamSource source = new StreamSource(byteInStream);
        SOAPMessage newMsg = newMsg = MESSAGE_FACTORY.createMessage();
        SOAPPart soapPart = newMsg.getSOAPPart();
        soapPart.setContent(source);
        return newMsg;
    }


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {

    }

    @AfterClass
    public static void oneTimeTearDown() {

    }

}
