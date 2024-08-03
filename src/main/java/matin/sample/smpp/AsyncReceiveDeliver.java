package matin.sample.smpp;

import org.jsmpp.bean.BindType;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;

import java.io.IOException;

public class AsyncReceiveDeliver {
    public static void main(String[] args) throws InterruptedException {
        String smppServer = "ipaddress"; // Replace with your SMPP server address
        int smppPort = 0; // Replace with your SMPP server port
        String systemId = "sysId"; // Replace with your system id
        String password = "pass"; // Replace with your password

        final SMPPSession session = new SMPPSession();
        try {
            session.connectAndBind(smppServer, smppPort, new BindParameter(BindType.BIND_RX, systemId, password, "VMA", TypeOfNumber.INTERNATIONAL, NumberingPlanIndicator.ISDN, null));
        } catch (IOException e) {
            System.err.println("Failed connect and bind to host");
            e.printStackTrace();
        }
        CustomMessageReceiverListener listener = new CustomMessageReceiverListener();
        // Set listener to receive deliver_sm
        session.setMessageReceiverListener(listener);

        Thread t = new Thread(listener);
        t.start();
        t.join();
        session.unbindAndClose();
    }
}