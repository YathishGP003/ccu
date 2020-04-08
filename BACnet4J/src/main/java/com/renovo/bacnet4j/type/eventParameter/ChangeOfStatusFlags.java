
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfStatusFlags extends AbstractEventParameter {
    public static final byte TYPE_ID = 18;

    private final UnsignedInteger timeDelay;
    private final StatusFlags selectedFlags;

    public ChangeOfStatusFlags(final UnsignedInteger timeDelay, final StatusFlags selectedFlags) {
        this.timeDelay = timeDelay;
        this.selectedFlags = selectedFlags;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, selectedFlags, 1);
    }

    public ChangeOfStatusFlags(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        selectedFlags = read(queue, StatusFlags.class, 1);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public StatusFlags getSelectedFlags() {
        return selectedFlags;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (selectedFlags == null ? 0 : selectedFlags.hashCode());
        result = prime * result + (timeDelay == null ? 0 : timeDelay.hashCode());
        return result;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ChangeOfStatusFlags other = (ChangeOfStatusFlags) obj;
        if (selectedFlags == null) {
            if (other.selectedFlags != null)
                return false;
        } else if (!selectedFlags.equals(other.selectedFlags))
            return false;
        if (timeDelay == null) {
            if (other.timeDelay != null)
                return false;
        } else if (!timeDelay.equals(other.timeDelay))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChangeOfStatusFlags[ timeDelay=" + timeDelay + ", selectedFlags=" + selectedFlags + ']';
    }
}
