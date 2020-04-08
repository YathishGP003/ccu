
package com.renovo.bacnet4j.type.notificationParameters;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfBitStringNotif extends AbstractNotificationParameter {
    public static final byte TYPE_ID = 0;

    private final BitString referencedBitstring;
    private final StatusFlags statusFlags;

    public ChangeOfBitStringNotif(final BitString referencedBitstring, final StatusFlags statusFlags) {
        this.referencedBitstring = referencedBitstring;
        this.statusFlags = statusFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, referencedBitstring, 0);
        write(queue, statusFlags, 1);
    }

    public ChangeOfBitStringNotif(final ByteQueue queue) throws BACnetException {
        referencedBitstring = read(queue, BitString.class, 0);
        statusFlags = read(queue, StatusFlags.class, 1);
    }

    public BitString getReferencedBitstring() {
        return referencedBitstring;
    }

    public StatusFlags getStatusFlags() {
        return statusFlags;
    }

    @Override
    public String toString() {
        return "ChangeOfBitStringNotif[ referencedBitstring=" + referencedBitstring + ", statusFlags=" + statusFlags + ']';
    }
    
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (referencedBitstring == null ? 0 : referencedBitstring.hashCode());
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
        final ChangeOfBitStringNotif other = (ChangeOfBitStringNotif) obj;
        if (referencedBitstring == null) {
            if (other.referencedBitstring != null)
                return false;
        } else if (!referencedBitstring.equals(other.referencedBitstring))
            return false;
        if (statusFlags == null) {
            if (other.statusFlags != null)
                return false;
        } else if (!statusFlags.equals(other.statusFlags))
            return false;
        return true;
    }
}
