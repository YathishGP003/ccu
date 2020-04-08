
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.type.primitive.Unsigned16;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class FDTEntry extends BaseType {
    private final OctetString bacnetipAddress;
    private final Unsigned16 timeToLive;
    private final Unsigned16 remainingTimeToLive;

    public FDTEntry(final OctetString bacnetipAddress, final Unsigned16 timeToLive,
            final Unsigned16 remainingTimeToLive) {
        this.bacnetipAddress = bacnetipAddress;
        this.timeToLive = timeToLive;
        this.remainingTimeToLive = remainingTimeToLive;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, bacnetipAddress, 0);
        write(queue, timeToLive, 1);
        write(queue, remainingTimeToLive, 2);
    }

    public FDTEntry(final ByteQueue queue) throws BACnetException {
        bacnetipAddress = read(queue, OctetString.class, 0);
        timeToLive = read(queue, Unsigned16.class, 1);
        remainingTimeToLive = read(queue, Unsigned16.class, 2);
    }

    public OctetString getBacnetipAddress() {
        return bacnetipAddress;
    }

    public Unsigned16 getTimeToLive() {
        return timeToLive;
    }

    public Unsigned16 getRemainingTimeToLive() {
        return remainingTimeToLive;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (bacnetipAddress == null ? 0 : bacnetipAddress.hashCode());
        result = prime * result + (remainingTimeToLive == null ? 0 : remainingTimeToLive.hashCode());
        result = prime * result + (timeToLive == null ? 0 : timeToLive.hashCode());
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
        final FDTEntry other = (FDTEntry) obj;
        if (bacnetipAddress == null) {
            if (other.bacnetipAddress != null)
                return false;
        } else if (!bacnetipAddress.equals(other.bacnetipAddress))
            return false;
        if (remainingTimeToLive == null) {
            if (other.remainingTimeToLive != null)
                return false;
        } else if (!remainingTimeToLive.equals(other.remainingTimeToLive))
            return false;
        if (timeToLive == null) {
            if (other.timeToLive != null)
                return false;
        } else if (!timeToLive.equals(other.timeToLive))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FDTEntry [bacnetipAddress=" + bacnetipAddress + ", timeToLive=" + timeToLive + ", remainingTimeToLive=" + remainingTimeToLive + ']';
    }
}
