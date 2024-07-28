package matin.sample.smpp;

import org.smpp.Data;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.*;

import java.io.IOException;

public class SMPPReceiver implements Runnable {
    private final String smppServer;
    private final int smppPort;
    private final String systemId;
    private final String password;

    public SMPPReceiver(String smppServer, int smppPort, String systemId, String password) {
        this.smppServer = smppServer;
        this.smppPort = smppPort;
        this.systemId = systemId;
        this.password = password;
    }

    @Override
    public void run() {
        TCPIPConnection connection = new TCPIPConnection(smppServer, smppPort);
        Session session = new Session(connection);
        try {
            BindRequest bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);

            BindResponse bindResponse = session.bind(bindRequest);

            if (bindResponse.getCommandStatus() == Data.ESME_ROK) {
                while (true) {
                    PDU pdu = session.receive();
                    if (pdu instanceof DeliverSM deliverSM) {
                        System.out.println("Received delivery report for message ID: " + deliverSM.getReceiptedMessageId());
                        // Respond to the delivery report
                        DeliverSMResp deliverSMResp = new DeliverSMResp();
                        session.respond(deliverSMResp);
                    } else if (pdu instanceof EnquireLink) {
                        EnquireLinkResp enquireLinkResp = new EnquireLinkResp();
                        session.respond(enquireLinkResp);
                    }
                }
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
}
