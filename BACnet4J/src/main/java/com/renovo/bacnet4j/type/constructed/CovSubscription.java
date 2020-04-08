
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class CovSubscription extends BaseType {
    private final RecipientProcess recipient;
    private final ObjectPropertyReference monitoredPropertyReference;
    private final Boolean issueConfirmedNotifications;
    private final UnsignedInteger timeRemaining;
    private final Real covIncrement;

    public CovSubscription(final RecipientProcess recipient, final ObjectPropertyReference monitoredPropertyReference,
            final Boolean issueConfirmedNotifications, final UnsignedInteger timeRemaining, final Real covIncrement) {
        this.recipient = recipient;
        this.monitoredPropertyReference = monitoredPropertyReference;
        this.issueConfirmedNotifications = issueConfirmedNotifications;
        this.timeRemaining = timeRemaining;
        this.covIncrement = covIncrement;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, recipient, 0);
        write(queue, monitoredPropertyReference, 1);
        write(queue, issueConfirmedNotifications, 2);
        write(queue, timeRemaining, 3);
        writeOptional(queue, covIncrement, 4);
    }

    public CovSubscription(final ByteQueue queue) throws BACnetException {
        recipient = read(queue, RecipientProcess.class, 0);
        monitoredPropertyReference = read(queue, ObjectPropertyReference.class, 1);
        issueConfirmedNotifications = read(queue, Boolean.class, 2);
        timeRemaining = read(queue, UnsignedInteger.class, 3);
        covIncrement = readOptional(queue, Real.class, 4);
    }

    public RecipientProcess getRecipient() {
        return recipient;
    }

    public ObjectPropertyReference getMonitoredPropertyReference() {
        return monitoredPropertyReference;
    }

    public Boolean getIssueConfirmedNotifications() {
        return issueConfirmedNotifications;
    }

    public UnsignedInteger getTimeRemaining() {
        return timeRemaining;
    }

    public Real getCovIncrement() {
        return covIncrement;
    }

    @Override
    public String toString() {
        return "CovSubscription [recipient=" + recipient + ", monitoredPropertyReference=" + monitoredPropertyReference
                + ", issueConfirmedNotifications=" + issueConfirmedNotifications + ", timeRemaining=" + timeRemaining
                + ", covIncrement=" + covIncrement + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (covIncrement == null ? 0 : covIncrement.hashCode());
        result = prime * result + (issueConfirmedNotifications == null ? 0 : issueConfirmedNotifications.hashCode());
        result = prime * result + (monitoredPropertyReference == null ? 0 : monitoredPropertyReference.hashCode());
        result = prime * result + (recipient == null ? 0 : recipient.hashCode());
        result = prime * result + (timeRemaining == null ? 0 : timeRemaining.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final CovSubscription other = (CovSubscription) obj;
        if (covIncrement == null) {
            if (other.covIncrement != null)
                return false;
        } else if (!covIncrement.equals(other.covIncrement))
            return false;
        if (issueConfirmedNotifications == null) {
            if (other.issueConfirmedNotifications != null)
                return false;
        } else if (!issueConfirmedNotifications.equals(other.issueConfirmedNotifications))
            return false;
        if (monitoredPropertyReference == null) {
            if (other.monitoredPropertyReference != null)
                return false;
        } else if (!monitoredPropertyReference.equals(other.monitoredPropertyReference))
            return false;
        if (recipient == null) {
            if (other.recipient != null)
                return false;
        } else if (!recipient.equals(other.recipient))
            return false;
        if (timeRemaining == null) {
            if (other.timeRemaining != null)
                return false;
        } else if (!timeRemaining.equals(other.timeRemaining))
            return false;
        return true;
    }
}
