
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.PropertyStates;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfStateNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 1;

    private final PropertyStates newState;
    private final StatusFlags statusFlags;

    public ChangeOfStateNotif(final PropertyStates newState, final StatusFlags statusFlags) {
        this.newState = newState;
        this.statusFlags = statusFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, newState, 0);
        write(queue, statusFlags, 1);
    }

    public ChangeOfStateNotif(final ByteQueue queue) throws BACnetException {
        newState = read(queue, PropertyStates.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
    }

    public PropertyStates getNewState() {
        return newState;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    @Override
    public String toString() {
        return "ChangeOfStateNotif [newState=" + newState + ", statusFlags=" + statusFlags + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (newState == null ? 0 : newState.hashCode());
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
        final ChangeOfStateNotif other = (ChangeOfStateNotif) obj;
        if (newState == null) {
            if (other.newState != null)
                return false;
        } else if (!newState.equals(other.newState))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
