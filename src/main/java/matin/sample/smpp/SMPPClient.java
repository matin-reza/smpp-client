package matin.sample.smpp;

import com.cloudhopper.commons.gsm.GsmUtil;
import net.freeutils.charset.SCGSMCharset;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.*;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.session.SubmitSmResult;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;


public class SMPPClient {
    SMPPSession session = new SMPPSession();
    String smppServer = "ipAdd"; // Replace with your SMPP server address
    int smppPort = 0; // Replace with your SMPP server port
    String systemId = "sysId"; // Replace with your system id
    String password = "pass"; // Replace with your password

    private boolean isHex(String sms) {
        return sms.matches("^[0-9A-F]+$");
    }

    private Encoding getCharset(String content) {
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

    private void sendSubmitSm(String originator, String number, DataCoding dataCoding, byte[] messageBody, ESMClass esmClass) throws ResponseTimeoutException, PDUException, IOException, InvalidResponseException, NegativeResponseException {
        String serviceType = "VMA";
        TypeOfNumber sourceAddrTon = TypeOfNumber.INTERNATIONAL;
        NumberingPlanIndicator sourceAddrNpi = NumberingPlanIndicator.ISDN;
        TypeOfNumber destAddrTon = TypeOfNumber.INTERNATIONAL;
        NumberingPlanIndicator destAddrNpi = NumberingPlanIndicator.ISDN;
        byte protocolId = 0x00; // Default protocol ID
        byte priorityFlag = 0x00; // Default priority
        String scheduleDeliveryTime = ""; // Optional, empty for immediate delivery
        String validityPeriod = ""; // Optional, empty for default validity
        RegisteredDelivery registeredDelivery = new RegisteredDelivery(1); // Request delivery receipt
        byte replaceIfPresentFlag = 0x00; // Default
        byte smDefaultMsgId = 0x00; // Default message ID

        SubmitSmResult submitResponse = session.submitShortMessage(
                serviceType,
                sourceAddrTon,
                sourceAddrNpi,
                originator,
                destAddrTon,
                destAddrNpi,
                number,
                esmClass,
                protocolId,
                priorityFlag,
                scheduleDeliveryTime,
                validityPeriod,
                registeredDelivery,
                replaceIfPresentFlag,
                dataCoding,
                smDefaultMsgId,
                messageBody);
        System.out.println("Message sent with message ID: " + submitResponse.getMessageId());
    }

    private void sendParts(byte[][] parts, DataCoding dataCoding, String originator, String number) throws PDUException, IOException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException {
        ESMClass esmClass = new ESMClass(64);
        for (byte[] part : parts)
            sendSubmitSm(originator, number, dataCoding, part, esmClass);
    }

    private void sendSimple(byte[] messageByte, DataCoding dataCoding, String originator, String number) throws PDUException, IOException, ResponseTimeoutException, InvalidResponseException, NegativeResponseException {
        sendSubmitSm(originator, number, dataCoding, messageByte, new ESMClass());
    }

    private void bindAndConnect() throws IOException {
        session.connectAndBind(smppServer, smppPort, new BindParameter(BindType.BIND_TX, systemId, password, "VMA", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, null, InterfaceVersion.IF_34));
        System.out.println("Successfully bound to SMPP server");
    }

    private DataCoding getCoding(Encoding encoding) {
        if (encoding.equals(Encoding.ASCII)) {
            return new DataCoding() {
                @Override
                public byte toByte() {
                    return 0;
                }
            }; // GSM 7-bit
        } else {
            return new DataCoding() {
                @Override
                public byte toByte() {
                    return 0x08;
                }
            }; // UCS-2
        }
    }

    private byte[] getMessageAsByteArray(String message, Encoding encoding) {
        if (encoding.equals(Encoding.ASCII))
            return message.getBytes();
        else
            return message.getBytes(StandardCharsets.UTF_16BE);
    }

    private int getBitLengthMessage(Encoding encoding, byte[] messageByte) {
        if (encoding.equals(Encoding.ASCII)) {
            return messageByte.length * 7;
        } else {
            return messageByte.length * 16;
        }
    }

    public void send(String originator, String number, String message) {
        Encoding encoding = getCharset(message);
        DataCoding dataCoding = getCoding(encoding);
        byte[] messageByte = getMessageAsByteArray(message, encoding);
        int contentBitsLen = getBitLengthMessage(encoding, messageByte);

        try {
            bindAndConnect();
            if (session.getSessionState() == SessionState.BOUND_TX) {
                if (isLargeMessage(contentBitsLen)) {
                    byte[][] parts = splitIntoParts(messageByte);
                    sendParts(parts, dataCoding, originator, number);
                } else
                    sendSimple(messageByte, dataCoding, originator, number);
            } else {
                System.err.println("Failed to bind to SMPP server: ");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ResponseTimeoutException | PDUException | InvalidResponseException | NegativeResponseException e) {
            throw new RuntimeException(e);
        } finally {
            session.unbindAndClose();
        }
    }

    public static void main(String[] args) {
        String messageText = "salam";
        SMPPClient client = new SMPPClient();
        String mobile = "desAddress";
        client.send("sourceAdd", mobile, messageText);
    }
}