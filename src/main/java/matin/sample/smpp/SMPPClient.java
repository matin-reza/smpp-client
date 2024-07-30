package matin.sample.smpp;

import com.cloudhopper.commons.gsm.GsmUtil;
import net.freeutils.charset.SCGSMCharset;
import org.smpp.*;
import org.smpp.pdu.*;
import org.smpp.util.ByteBuffer;
import org.smpp.util.NotEnoughDataInByteBufferException;
import org.smpp.util.TerminatingZeroNotFoundException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class SMPPClient {

    private boolean isHex(String sms) {
        return sms.matches("^[0-9A-F]+$");
    }

    private Encoding getCharset(String content) throws UnsupportedEncodingException {
        Encoding result = null;
        String ASCIIChecked;
        String UTF8Checked;
        String UTF_8 = "UTF-8";
        Charset ASCIICharset = new SCGSMCharset();
        Charset UTF8Charset = Charset.forName(UTF_8);
        ASCIIChecked = new String(content.getBytes(ASCIICharset), ASCIICharset);
        UTF8Checked = new String(content.getBytes(UTF8Charset), UTF8Charset);
        if (content.equals(ASCIIChecked)) {
            result = Encoding.ASCII;
        } else if (content.equals(UTF8Checked)) {
            result = Encoding.UCS2;
        } else if (isHex(content)) {
            result = Encoding.BINARY;
        }
        return result;
    }

    private boolean isLargeMessage(int messageBitsLen) {
        return messageBitsLen > 1120;
    }

    private byte[][] splitIntoParts(byte[] message) {
        Random random = new Random();
        int refNum = random.nextInt(255);
        return GsmUtil.createConcatenatedBinaryShortMessages(message, (byte) refNum);
    }

    private Address prepareAddress(String number) throws WrongLengthOfStringException {
        Address address = new Address(number);
        address.setNpi((byte) 1);
        address.setTon((byte) 1);
        return address;
    }

    private SubmitSM prepareSubmitSM(byte[] messageByte, byte dataCoding, String originator, String number) throws PDUException, NotEnoughDataInByteBufferException, TerminatingZeroNotFoundException {
        SubmitSM submit = new SubmitSM();
        submit.setSourceAddr(prepareAddress(originator));
        submit.setDestAddr(prepareAddress(number));
        submit.setDataCoding(dataCoding);
        submit.setRegisteredDelivery((byte) 0);
        ByteBuffer sms = new ByteBuffer();
        sms.setBuffer(messageByte);
        submit.setShortMessageData(sms);
        return submit;
    }

    private void sendParts(byte[][] parts, byte dataCoding, Session session, String originator, String number) throws PDUException, NotEnoughDataInByteBufferException, TerminatingZeroNotFoundException, WrongSessionStateException, IOException, TimeoutException {
        for (byte[] part : parts) {
            SubmitSM submit = prepareSubmitSM(part, dataCoding, originator, number);
            submit.setEsmClass((byte) 64);
            SubmitSMResp submitResponse = session.submit(submit);
            if (submitResponse.getCommandStatus() == Data.ESME_ROK) {
                System.out.println("Message sent successfully!");
            } else {
                System.err.println("Error sending message: " + submitResponse.getCommandStatus());
            }
        }
    }

    private void sendSimple(byte[] messageByte, byte dataCoding, Session session, String originator, String number) throws WrongSessionStateException, PDUException, IOException, TimeoutException, NotEnoughDataInByteBufferException, TerminatingZeroNotFoundException {
        SubmitSM submit = prepareSubmitSM(messageByte, dataCoding, originator, number);
        SubmitSMResp submitResponse = session.submit(submit);
        if (submitResponse.getCommandStatus() == Data.ESME_ROK) {
            System.out.println("Message sent successfully!" + submitResponse.getMessageId());
        } else {
            System.err.println("Error sending message: " + submitResponse.getCommandStatus());
        }
    }

    private void sendMessage(String smppServer, int smppPort, String systemId, String password) throws UnsupportedEncodingException {
        byte dataCoding;
        String sourceAddress = "your source"; // Replace with your source address
        String destinationAddress = "your des"; // Replace with the destination address
        String messageText = "salam"; // Replace with your message text
        int contentBitsLen;
        byte[] messageByte;

        Encoding encoding = getCharset(messageText);


        if (encoding.equals(Encoding.ASCII)) {
            dataCoding = 0x00; // GSM 7-bit
            messageByte = messageText.getBytes();
            contentBitsLen = messageByte.length * 7;
        } else {
            dataCoding = 0x08; // UCS-2
            messageByte = messageText.getBytes(StandardCharsets.UTF_16BE);
            contentBitsLen = messageByte.length * 16;
        }

        TCPIPConnection connection = new TCPIPConnection(smppServer, smppPort);
        Session session = new Session(connection);

        try {
            BindRequest bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);
            BindResponse bindResponse = session.bind(bindRequest);

            if (bindResponse.getCommandStatus() == Data.ESME_ROK) {
                if (isLargeMessage(contentBitsLen)) {
                    byte[][] parts = splitIntoParts(messageByte);
                    sendParts(parts, dataCoding, session, sourceAddress, destinationAddress);
                } else
                    sendSimple(messageByte, dataCoding, session, sourceAddress, destinationAddress);
            } else {
                System.err.println("Failed to bind to SMPP server: " + bindResponse.getCommandStatus());
            }

            session.unbind();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public static void main(String[] args) throws UnsupportedEncodingException {
        String smppServer = "localhost"; // Replace with your SMPP server address
        int smppPort = 2775; // Replace with your SMPP server port
        String systemId = "sysId"; // Replace with your system id
        String password = "password"; // Replace with your password
        SMPPClient client = new SMPPClient();
        SMPPReceiver receiver = new SMPPReceiver(smppServer, smppPort, systemId, password);
        Thread t = new Thread(receiver);
        //t.start();

        client.sendMessage(smppServer, smppPort, systemId, password);
    }
}
