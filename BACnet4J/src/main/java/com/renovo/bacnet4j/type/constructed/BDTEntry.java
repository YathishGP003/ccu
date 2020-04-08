
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class BDTEntry extends BaseType {
    private final HostNPort bbmdAddress;
    private final OctetString broadcastMask; // optional

    public BDTEntry(final HostNPort bbmdAddress, final OctetString broadcastMask) {
        this.bbmdAddress = bbmdAddress;
        this.broadcastMask = broadcastMask;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, bbmdAddress, 0);
        writeOptional(queue, broadcastMask, 1);
    }

    public BDTEntry(final ByteQueue queue) throws BACnetException {
        bbmdAddress = read(queue, HostNPort.class, 0);
        broadcastMask = readOptional(queue, OctetString.class, 1);
    }

    public HostNPort getBbmdAddress() {
        return bbmdAddress;
    }

    public OctetString getBroadcastMask() {
        return broadcastMask;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (bbmdAddress == null ? 0 : bbmdAddress.hashCode());
        result = prime * result + (broadcastMask == null ? 0 : broadcastMask.hashCode());
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
        final BDTEntry other = (BDTEntry) obj;
        if (bbmdAddress == null) {
            if (other.bbmdAddress != null)
                return false;
        } else if (!bbmdAddress.equals(other.bbmdAddress))
            return false;
        if (broadcastMask == null) {
            if (other.broadcastMask != null)
                return false;
        } else if (!broadcastMask.equals(other.broadcastMask))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BDTEntry [bbmdAddress=" + bbmdAddress + ", broadcastMask=" + broadcastMask + ']';
    }
}
