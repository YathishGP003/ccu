
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class UnsignedOutOfRangeNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 16;

    private final UnsignedInteger exceedingValue;
    private final StatusFlags statusFlags;
    private final UnsignedInteger deadband;
    private final UnsignedInteger exceedingLimit;

    public UnsignedOutOfRangeNotif(final UnsignedInteger exceedingValue, final StatusFlags statusFlags,
            final UnsignedInteger deadband, final UnsignedInteger exceedingLimit) {
        this.exceedingValue = exceedingValue;
        this.statusFlags = statusFlags;
        this.deadband = deadband;
        this.exceedingLimit = exceedingLimit;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, exceedingValue, 0);
        write(queue, statusFlags, 1);
        write(queue, deadband, 2);
        write(queue, exceedingLimit, 3);
    }

    public UnsignedOutOfRangeNotif(final ByteQueue queue) throws BACnetException {
        exceedingValue = read(queue, UnsignedInteger.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        deadband = read(queue, UnsignedInteger.class, 2);
        exceedingLimit = read(queue, UnsignedInteger.class, 3);
    }

    public UnsignedInteger getExceedingValue() {
        return exceedingValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public UnsignedInteger getDeadband() {
        return deadband;
    }

    public UnsignedInteger getExceedingLimit() {
        return exceedingLimit;
    }

    @Override
    public String toString() {
        return "UnsignedOutOfRangeNotif[ exceedingValue=" + exceedingValue + ", statusFlags=" + statusFlags + ", deadband=" + deadband + ", exceedingLimit=" + exceedingLimit + ']';
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (deadband == null ? 0 : deadband.hashCode());
        result = prime * result + (exceedingLimit == null ? 0 : exceedingLimit.hashCode());
        result = prime * result + (exceedingValue == null ? 0 : exceedingValue.hashCode());
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
        final UnsignedOutOfRangeNotif other = (UnsignedOutOfRangeNotif) obj;
        if (deadband == null) {
            if (other.deadband != null)
                return false;
        } else if (!deadband.equals(other.deadband))
            return false;
        if (exceedingLimit == null) {
            if (other.exceedingLimit != null)
                return false;
        } else if (!exceedingLimit.equals(other.exceedingLimit))
            return false;
        if (exceedingValue == null) {
            if (other.exceedingValue != null)
                return false;
        } else if (!exceedingValue.equals(other.exceedingValue))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
