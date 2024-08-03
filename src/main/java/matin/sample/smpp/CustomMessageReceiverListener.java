package matin.sample.smpp;

import org.jsmpp.bean.*;
import org.jsmpp.extra.ProcessRequestException;
import org.jsmpp.session.DataSmResult;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.Session;
import org.jsmpp.util.InvalidDeliveryReceiptException;

public class CustomMessageReceiverListener implements MessageReceiverListener, Runnable {
    @Override
    public void onAcceptDeliverSm(DeliverSm deliverSm) throws ProcessRequestException {
        try {
            if (MessageType.SMSC_DEL_RECEIPT.containedIn(deliverSm.getEsmClass())) {
                // Extract delivery receipt details
                DeliveryReceipt delReceipt = deliverSm.getShortMessageAsDeliveryReceipt();
                System.out.println("Received delivery report for message ID: " + delReceipt.getId());
                System.out.println("Delivery status: " + delReceipt.getFinalStatus());
                System.out.println("Delivery status: " + delReceipt.getError());
            } else {
                System.out.println("Received message: " + new String(deliverSm.getShortMessage()));
            }
        } catch (InvalidDeliveryReceiptException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onAcceptAlertNotification(AlertNotification alertNotification) {
    }

    @Override
    public DataSmResult onAcceptDataSm(DataSm dataSm, Session session) throws ProcessRequestException {
        return null;
    }

    @Override
    public void run() {
        while (true) {
        }
    }
}
