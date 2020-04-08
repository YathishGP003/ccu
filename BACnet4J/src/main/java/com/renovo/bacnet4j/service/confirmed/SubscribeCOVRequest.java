
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetRuntimeException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class SubscribeCOVRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 5;

    private final UnsignedInteger subscriberProcessIdentifier;
    private final ObjectIdentifier monitoredObjectIdentifier;
    private final Boolean issueConfirmedNotifications;
    private final UnsignedInteger lifetime;

    public SubscribeCOVRequest(final UnsignedInteger subscriberProcessIdentifier,
            final ObjectIdentifier monitoredObjectIdentifier, final Boolean issueConfirmedNotifications,
            final UnsignedInteger lifetime) {
        if (subscriberProcessIdentifier.intValue() == 0)
            throw new BACnetRuntimeException("Cannot use 0 as subscriberProcessIdentifier. See 13.1.1");

        this.subscriberProcessIdentifier = subscriberProcessIdentifier;
        this.monitoredObjectIdentifier = monitoredObjectIdentifier;
        this.issueConfirmedNotifications = issueConfirmedNotifications;
        this.lifetime = lifetime;
    }

    public SubscribeCOVRequest(final SubscribeCOVRequest subscription) {
        this(subscription.subscriberProcessIdentifier, subscription.monitoredObjectIdentifier, null, null);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, subscriberProcessIdentifier, 0);
        write(queue, monitoredObjectIdentifier, 1);
        writeOptional(queue, issueConfirmedNotifications, 2);
        writeOptional(queue, lifetime, 3);
    }

    SubscribeCOVRequest(final ByteQueue queue) throws BACnetException {
        subscriberProcessIdentifier = read(queue, UnsignedInteger.class, 0);
        monitoredObjectIdentifier = read(queue, ObjectIdentifier.class, 1);
        issueConfirmedNotifications = readOptional(queue, Boolean.class, 2);
        lifetime = readOptional(queue, UnsignedInteger.class, 3);
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        try {
            final BACnetObject obj = localDevice.getObjectRequired(monitoredObjectIdentifier);

            if (issueConfirmedNotifications == null && lifetime == null)
                obj.removeCovSubscription(from, subscriberProcessIdentifier, null);
            else
                obj.addCovSubscription(from, subscriberProcessIdentifier, issueConfirmedNotifications, lifetime, null,
                        null);
            return null;
        } catch (final BACnetServiceException e) {
            throw new BACnetErrorException(getChoiceId(), e);
        }
    }

    public UnsignedInteger getSubscriberProcessIdentifier() {
        return subscriberProcessIdentifier;
    }

    public ObjectIdentifier getMonitoredObjectIdentifier() {
        return monitoredObjectIdentifier;
    }

    public Boolean getIssueConfirmedNotifications() {
        return issueConfirmedNotifications;
    }

    public UnsignedInteger getLifetime() {
        return lifetime;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (issueConfirmedNotifications == null ? 0 : issueConfirmedNotifications.hashCode());
        result = PRIME * result + (lifetime == null ? 0 : lifetime.hashCode());
        result = PRIME * result + (monitoredObjectIdentifier == null ? 0 : monitoredObjectIdentifier.hashCode());
        result = PRIME * result + (subscriberProcessIdentifier == null ? 0 : subscriberProcessIdentifier.hashCode());
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
        final SubscribeCOVRequest other = (SubscribeCOVRequest) obj;
        if (issueConfirmedNotifications == null) {
            if (other.issueConfirmedNotifications != null)
                return false;
        } else if (!issueConfirmedNotifications.equals(other.issueConfirmedNotifications))
            return false;
        if (lifetime == null) {
            if (other.lifetime != null)
                return false;
        } else if (!lifetime.equals(other.lifetime))
            return false;
        if (monitoredObjectIdentifier == null) {
            if (other.monitoredObjectIdentifier != null)
                return false;
        } else if (!monitoredObjectIdentifier.equals(other.monitoredObjectIdentifier))
            return false;
        if (subscriberProcessIdentifier == null) {
            if (other.subscriberProcessIdentifier != null)
                return false;
        } else if (!subscriberProcessIdentifier.equals(other.subscriberProcessIdentifier))
            return false;
        return true;
    }
}
