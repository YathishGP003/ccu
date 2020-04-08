
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class FloatingLimitNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 4;

    private final Real referenceValue;
    private final StatusFlags statusFlags;
    private final Real setpointValue;
    private final Real errorLimit;

    public FloatingLimitNotif(final Real referenceValue, final StatusFlags statusFlags, final Real setpointValue,
            final Real errorLimit) {
        this.referenceValue = referenceValue;
        this.statusFlags = statusFlags;
        this.setpointValue = setpointValue;
        this.errorLimit = errorLimit;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, referenceValue, 0);
        write(queue, statusFlags, 1);
        write(queue, setpointValue, 2);
        write(queue, errorLimit, 3);
    }

    public FloatingLimitNotif(final ByteQueue queue) throws BACnetException {
        referenceValue = read(queue, Real.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        setpointValue = read(queue, Real.class, 2);
        errorLimit = read(queue, Real.class, 3);
    }

    public Real getReferenceValue() {
        return referenceValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public Real getSetpointValue() {
        return setpointValue;
    }

    public Real getErrorLimit() {
        return errorLimit;
    }

    @Override
    public String toString() {
        return "FloatingLimitNotif[ referenceValue=" + referenceValue + ", statusFlags=" + statusFlags + ", setpointValue=" + setpointValue + ", errorLimit=" + errorLimit + ']';
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (errorLimit == null ? 0 : errorLimit.hashCode());
        result = PRIME * result + (referenceValue == null ? 0 : referenceValue.hashCode());
        result = PRIME * result + (setpointValue == null ? 0 : setpointValue.hashCode());
        result = PRIME * result + (statusFlags == null ? 0 : statusFlags.hashCode());
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
        final FloatingLimitNotif other = (FloatingLimitNotif) obj;
        if (errorLimit == null) {
            if (other.errorLimit != null)
                return false;
        } else if (!errorLimit.equals(other.errorLimit))
            return false;
        if (referenceValue == null) {
            if (other.referenceValue != null)
                return false;
        } else if (!referenceValue.equals(other.referenceValue))
            return false;
        if (setpointValue == null) {
            if (other.setpointValue != null)
                return false;
        } else if (!setpointValue.equals(other.setpointValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
