
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class FloatingLimit extends AbstractEventParameter {
    public static final byte TYPE_ID = 4;

    private final UnsignedInteger timeDelay;
    private final DeviceObjectPropertyReference setpointReference;
    private final Real lowDiffLimit;
    private final Real highDiffLimit;
    private final Real deadband;

    public FloatingLimit(final UnsignedInteger timeDelay, final DeviceObjectPropertyReference setpointReference,
            final Real lowDiffLimit, final Real highDiffLimit, final Real deadband) {
        this.timeDelay = timeDelay;
        this.setpointReference = setpointReference;
        this.lowDiffLimit = lowDiffLimit;
        this.highDiffLimit = highDiffLimit;
        this.deadband = deadband;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, setpointReference, 1);
        write(queue, lowDiffLimit, 2);
        write(queue, highDiffLimit, 3);
        write(queue, deadband, 4);
    }

    public FloatingLimit(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        setpointReference = read(queue, DeviceObjectPropertyReference.class, 1);
        lowDiffLimit = read(queue, Real.class, 2);
        highDiffLimit = read(queue, Real.class, 3);
        deadband = read(queue, Real.class, 4);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public DeviceObjectPropertyReference getSetpointReference() {
        return setpointReference;
    }

    public Real getLowDiffLimit() {
        return lowDiffLimit;
    }

    public Real getHighDiffLimit() {
        return highDiffLimit;
    }

    public Real getDeadband() {
        return deadband;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public DeviceObjectPropertyReference getReference() {
        return setpointReference;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deadband == null ? 0 : deadband.hashCode());
        result = PRIME * result + (highDiffLimit == null ? 0 : highDiffLimit.hashCode());
        result = PRIME * result + (lowDiffLimit == null ? 0 : lowDiffLimit.hashCode());
        result = PRIME * result + (setpointReference == null ? 0 : setpointReference.hashCode());
        result = PRIME * result + (timeDelay == null ? 0 : timeDelay.hashCode());
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
        final FloatingLimit other = (FloatingLimit) obj;
        if (deadband == null) {
            if (other.deadband != null)
                return false;
        } else if (!deadband.equals(other.deadband))
            return false;
        if (highDiffLimit == null) {
            if (other.highDiffLimit != null)
                return false;
        } else if (!highDiffLimit.equals(other.highDiffLimit))
            return false;
        if (lowDiffLimit == null) {
            if (other.lowDiffLimit != null)
                return false;
        } else if (!lowDiffLimit.equals(other.lowDiffLimit))
            return false;
        if (setpointReference == null) {
            if (other.setpointReference != null)
                return false;
        } else if (!setpointReference.equals(other.setpointReference))
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
        return "FloatingLimit[ timeDelay=" + timeDelay + ", setpointReference=" + setpointReference + ", lowDiffLimit=" + lowDiffLimit + ", highDiffLimit=" + highDiffLimit + ", deadband=" + deadband + ']';
    }
}
