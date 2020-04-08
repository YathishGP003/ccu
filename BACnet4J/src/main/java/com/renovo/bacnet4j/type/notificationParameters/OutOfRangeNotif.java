
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class OutOfRangeNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 5;

    private final Real exceedingValue;
    private final StatusFlags statusFlags;
    private final Real deadband;
    private final Real exceededLimit;

    public OutOfRangeNotif(final Real exceedingValue, final StatusFlags statusFlags, final Real deadband,
            final Real exceededLimit) {
        this.exceedingValue = exceedingValue;
        this.statusFlags = statusFlags;
        this.deadband = deadband;
        this.exceededLimit = exceededLimit;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, exceedingValue, 0);
        write(queue, statusFlags, 1);
        write(queue, deadband, 2);
        write(queue, exceededLimit, 3);
    }

    public OutOfRangeNotif(final ByteQueue queue) throws BACnetException {
        exceedingValue = read(queue, Real.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
        deadband = read(queue, Real.class, 2);
        exceededLimit = read(queue, Real.class, 3);
    }

    public Real getExceedingValue() {
        return exceedingValue;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public Real getDeadband() {
        return deadband;
    }

    public Real getExceededLimit() {
        return exceededLimit;
    }

    @Override
    public String toString() {
        return "OutOfRangeNotif [exceedingValue=" + exceedingValue + ", statusFlags=" + statusFlags + ", deadband="
                + deadband + ", exceededLimit=" + exceededLimit + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deadband == null ? 0 : deadband.hashCode());
        result = PRIME * result + (exceededLimit == null ? 0 : exceededLimit.hashCode());
        result = PRIME * result + (exceedingValue == null ? 0 : exceedingValue.hashCode());
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
        final OutOfRangeNotif other = (OutOfRangeNotif) obj;
        if (deadband == null) {
            if (other.deadband != null)
                return false;
        } else if (!deadband.equals(other.deadband))
            return false;
        if (exceededLimit == null) {
            if (other.exceededLimit != null)
                return false;
        } else if (!exceededLimit.equals(other.exceededLimit))
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
