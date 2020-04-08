
package com.renovo.bacnet4j.service.unconfirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class UTCTimeSynchronizationRequest extends UnconfirmedRequestService {
    public static final byte TYPE_ID = 9;

    private final DateTime time;

    public UTCTimeSynchronizationRequest(final DateTime time) {
        this.time = time;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, time);
    }

    UTCTimeSynchronizationRequest(final ByteQueue queue) throws BACnetException {
        time = read(queue, DateTime.class);
    }

    @Override
    public void handle(final LocalDevice localDevice, final Address from) {
        localDevice.getEventHandler().synchronizeTime(from, time, true);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (time == null ? 0 : time.hashCode());
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
        final UTCTimeSynchronizationRequest other = (UTCTimeSynchronizationRequest) obj;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        return true;
    }
}
