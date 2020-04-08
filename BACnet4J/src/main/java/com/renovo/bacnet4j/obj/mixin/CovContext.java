
package com.renovo.bacnet4j.obj.mixin;

import org.threeten.bp.Clock;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class CovContext {
    private final Clock clock;

    // Identifying properties.
    private final Address address;
    private final UnsignedInteger subscriberProcessIdentifier;
    // This is the property that was requested to be monitored in the subscription request. Can be null.
    private final PropertyIdentifier monitoredProperty;
    // This is the property that will be reported as being monitored in the active COV subscriptions property in the
    // device. Cannot be null.
    private final PropertyIdentifier exposedMonitoredProperty;

    // Mutable properties.
    private boolean issueConfirmedNotifications;
    private long expiryTime;
    private Real covIncrement;

    // Runtime values.
    private Encodable lastCovIncrementValue;

    public CovContext(final Clock clock, final Address address, final UnsignedInteger subscriberProcessIdentifier,
            final PropertyIdentifier monitoredProperty, final PropertyIdentifier exposedMonitoredProperty) {
        this.clock = clock;
        this.address = address;
        this.subscriberProcessIdentifier = subscriberProcessIdentifier;
        this.monitoredProperty = monitoredProperty;
        this.exposedMonitoredProperty = exposedMonitoredProperty;
    }

    public Address getAddress() {
        return address;
    }

    public UnsignedInteger getSubscriberProcessIdentifier() {
        return subscriberProcessIdentifier;
    }

    public PropertyIdentifier getMonitoredProperty() {
        return monitoredProperty;
    }

    public boolean isObjectSubscription() {
        return monitoredProperty == null;
    }

    public PropertyIdentifier getExposedMonitoredProperty() {
        return exposedMonitoredProperty;
    }

    public boolean isIssueConfirmedNotifications() {
        return issueConfirmedNotifications;
    }

    public void setIssueConfirmedNotifications(final boolean issueConfirmedNotifications) {
        this.issueConfirmedNotifications = issueConfirmedNotifications;
    }

    public Real getCovIncrement() {
        return covIncrement;
    }

    public void setCovIncrement(final Real covIncrement) {
        this.covIncrement = covIncrement;
    }

    public void setExpiryTime(final int seconds) {
        if (seconds == 0)
            expiryTime = -1;
        else
            expiryTime = clock.millis() + seconds * 1000;
    }

    public boolean hasExpired(final long now) {
        if (expiryTime == -1)
            return false;
        return expiryTime < now;
    }

    public Encodable getLastCovIncrementValue() {
        return lastCovIncrementValue;
    }

    public void setLastCovIncrementValue(final Encodable lastCovIncrementValue) {
        this.lastCovIncrementValue = lastCovIncrementValue;
    }

    public int getSecondsRemaining(final long now) {
        if (expiryTime == -1)
            return 0;
        final int left = (int) ((expiryTime - now + 500) / 1000);
        if (left < 1)
            return 1;
        return left;
    }
}
