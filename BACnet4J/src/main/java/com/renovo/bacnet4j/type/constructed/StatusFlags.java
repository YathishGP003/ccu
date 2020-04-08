
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class StatusFlags extends BitString {
    public StatusFlags(final boolean inAlarm, final boolean fault, final boolean overridden,
            final boolean outOfService) {
        super(new boolean[] { inAlarm, fault, overridden, outOfService });
    }

    public StatusFlags(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public boolean isInAlarm() {
        return getValue()[0];
    }

    public void setInAlarm(final boolean b) {
        getValue()[0] = b;
    }

    public boolean isFault() {
        return getValue()[1];
    }

    public void setFault(final boolean b) {
        getValue()[1] = b;
    }

    public boolean isOverridden() {
        return getValue()[2];
    }

    public void setOverridden(final boolean b) {
        getValue()[2] = b;
    }

    public boolean isOutOfService() {
        return getValue()[3];
    }

    public void setOutOfService(final boolean b) {
        getValue()[3] = b;
    }

    @Override
    public String toString() {
        return "StatusFlags [in-alarm=" + isInAlarm() + ", fault=" + isFault() + ", overridden=" + isOverridden() + ", out-of-service=" + isOutOfService() + "]";
    }
}
