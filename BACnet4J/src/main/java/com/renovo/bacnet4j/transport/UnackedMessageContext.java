
package com.renovo.bacnet4j.transport;

import org.threeten.bp.Clock;
import java.util.Arrays;

import com.renovo.bacnet4j.ResponseConsumer;
import com.renovo.bacnet4j.apdu.APDU;
import com.renovo.bacnet4j.apdu.Segmentable;
import com.renovo.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class UnackedMessageContext {
    private long deadline;
    private int attemptsLeft;

    private final Clock clock;

    // Temporarily add to the context for troubleshooting.
    private final ConfirmedRequestService service;

    // The response consumer, for confirmed requests
    private final ResponseConsumer consumer;

    // The original APDU for resending in case of timeout.
    private APDU originalApdu;

    // Segment info for receiving segmented messages.
    private SegmentWindow segmentWindow;
    private Segmentable segmentedMessage;

    // Segment info for sending segmented messages.
    private Segmentable segmentTemplate;
    private ByteQueue serviceData;
    private byte[] segBuf;
    private int lastIdSent;

    public UnackedMessageContext(final Clock clock, final int timeout, final int retries,
            final ResponseConsumer consumer, final ConfirmedRequestService service) {
        this.clock = clock;
        reset(timeout, retries);
        this.consumer = consumer;
        this.service = service;
    }

    public void retry(final int timeout) {
        this.deadline = clock.millis() + timeout;
        attemptsLeft--;
    }

    public void reset(final int timeout, final int retries) {
        this.deadline = clock.millis() + timeout;
        this.attemptsLeft = retries;
    }

    public long getDeadline() {
        return deadline;
    }

    public boolean hasMoreAttempts() {
        return attemptsLeft > 0;
    }

    public ResponseConsumer getConsumer() {
        return consumer;
    }

    public ConfirmedRequestService getService() {
        return service;
    }

    public APDU getOriginalApdu() {
        return originalApdu;
    }

    public void setOriginalApdu(final APDU originalApdu) {
        this.originalApdu = originalApdu;
    }

    public SegmentWindow getSegmentWindow() {
        return segmentWindow;
    }

    public void setSegmentWindow(final SegmentWindow segmentWindow) {
        this.segmentWindow = segmentWindow;
    }

    public Segmentable getSegmentedMessage() {
        return segmentedMessage;
    }

    public void setSegmentedMessage(final Segmentable segmentedResponse) {
        this.segmentedMessage = segmentedResponse;
    }

    public boolean isExpired(final long now) {
        return deadline < now;
    }

    public Segmentable getSegmentTemplate() {
        return segmentTemplate;
    }

    public void setSegmentTemplate(final Segmentable segmentTemplate) {
        this.segmentTemplate = segmentTemplate;
    }

    public ByteQueue getServiceData() {
        return serviceData;
    }

    public void setServiceData(final ByteQueue serviceData) {
        this.serviceData = serviceData;
    }

    public void setSegBuf(final byte[] segBuf) {
        this.segBuf = segBuf;
    }

    public ByteQueue getNextSegment() {
        final int count = serviceData.pop(segBuf);
        return new ByteQueue(segBuf, 0, count);
    }

    public int getLastIdSent() {
        return lastIdSent;
    }

    public void setLastIdSent(final int lastIdSent) {
        this.lastIdSent = lastIdSent;
    }

    public void useConsumer(final ConsumerClient client) {
        if (consumer != null) {
            client.use(consumer);
        }
    }

    @Override
    public String toString() {
        return "UnackedMessageContext [deadline=" + deadline + ", attemptsLeft=" + attemptsLeft + ", clock=" + clock
                + ", service=" + service + ", consumer=" + consumer + ", originalApdu=" + originalApdu
                + ", segmentWindow=" + segmentWindow + ", segmentedMessage=" + segmentedMessage + ", segmentTemplate="
                + segmentTemplate + ", serviceData=" + serviceData + ", segBuf=" + Arrays.toString(segBuf)
                + ", lastIdSent=" + lastIdSent + "]";
    }

    @FunctionalInterface
    public static interface ConsumerClient {
        void use(ResponseConsumer consumer);
    }
}
