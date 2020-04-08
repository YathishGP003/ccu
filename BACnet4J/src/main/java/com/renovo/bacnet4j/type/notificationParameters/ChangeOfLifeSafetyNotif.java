
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.LifeSafetyMode;
import com.renovo.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.renovo.bacnet4j.type.enumerated.LifeSafetyState;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfLifeSafetyNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 8;

    private final LifeSafetyState newState;
    private final LifeSafetyMode newMode;
    private final StatusFlags statusFlags;
    private final LifeSafetyOperation operationExpected;

    public ChangeOfLifeSafetyNotif(final LifeSafetyState newState, final LifeSafetyMode newMode,
            final StatusFlags statusFlags, final LifeSafetyOperation operationExpected) {
        this.newState = newState;
        this.newMode = newMode;
        this.statusFlags = statusFlags;
        this.operationExpected = operationExpected;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, newState, 0);
        write(queue, newMode, 1);
        write(queue, statusFlags, 2);
        write(queue, operationExpected, 3);
    }

    public ChangeOfLifeSafetyNotif(final ByteQueue queue) throws BACnetException {
        newState = read(queue, LifeSafetyState.class, 0);
        newMode = read(queue, LifeSafetyMode.class, 1);
        statusFlags = read(queue, StatusFlags.class, 2);
        operationExpected = read(queue, LifeSafetyOperation.class, 3);
    }

    public LifeSafetyState getNewState() {
        return newState;
    }

    public LifeSafetyMode getNewMode() {
        return newMode;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    public LifeSafetyOperation getOperationExpected() {
        return operationExpected;
    }

    @Override
    public String toString() {
        return "ChangeOfLifeSafetyNotif [newState=" + newState + ", newMode=" + newMode + ", statusFlags=" + statusFlags
                + ", operationExpected=" + operationExpected + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (newMode == null ? 0 : newMode.hashCode());
        result = PRIME * result + (newState == null ? 0 : newState.hashCode());
        result = PRIME * result + (operationExpected == null ? 0 : operationExpected.hashCode());
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
        final ChangeOfLifeSafetyNotif other = (ChangeOfLifeSafetyNotif) obj;
        if (newMode == null) {
            if (other.newMode != null)
                return false;
        } else if (!newMode.equals(other.newMode))
            return false;
        if (newState == null) {
            if (other.newState != null)
                return false;
        } else if (!newState.equals(other.newState))
            return false;
        if (operationExpected == null) {
            if (other.operationExpected != null)
                return false;
        } else if (!operationExpected.equals(other.operationExpected))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
