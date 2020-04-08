
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfReliabilityNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 19;

    private final Reliability reliability;
    private final StatusFlags statusFlags;
    private final SequenceOf<PropertyValue> propertyValues;

    public ChangeOfReliabilityNotif(final Reliability reliability, final StatusFlags statusFlags,
            final SequenceOf<PropertyValue> propertyValues) {
        this.reliability = reliability;
        this.statusFlags = statusFlags;
        this.propertyValues = propertyValues;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, reliability, 0);
        write(queue, statusFlags, 1);
        write(queue, propertyValues, 2);
    }

    public ChangeOfReliabilityNotif(final ByteQueue queue) throws BACnetException {
        reliability = read(queue, Reliability.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        propertyValues = readSequenceOf(queue, PropertyValue.class, 2);
    }

    public Reliability getReliability() {
        return reliability;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public SequenceOf<PropertyValue> getPropertyValues() {
        return propertyValues;
    }

    @Override
    public String toString() {
        return "ChangeOfReliabilityNotif [reliability=" + reliability + ", statusFlags=" + statusFlags
                + ", propertyValues=" + propertyValues + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (propertyValues == null ? 0 : propertyValues.hashCode());
        result = prime * result + (reliability == null ? 0 : reliability.hashCode());
        result = prime * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final ChangeOfReliabilityNotif other = (ChangeOfReliabilityNotif) obj;
        if (propertyValues == null) {
            if (other.propertyValues != null)
                return false;
        } else if (!propertyValues.equals(other.propertyValues))
            return false;
        if (reliability == null) {
            if (other.reliability != null)
                return false;
        } else if (!reliability.equals(other.reliability))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
