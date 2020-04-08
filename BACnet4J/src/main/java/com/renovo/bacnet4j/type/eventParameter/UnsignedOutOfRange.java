
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class UnsignedOutOfRange extends AbstractEventParameter {
    public static final byte TYPE_ID = 16;

    private final UnsignedInteger timeDelay;
    private final UnsignedInteger lowLimit;
    private final UnsignedInteger highLimit;
    private final UnsignedInteger deadband;

    public UnsignedOutOfRange(final UnsignedInteger timeDelay, final UnsignedInteger lowLimit,
            final UnsignedInteger highLimit, final UnsignedInteger deadband) {
        this.timeDelay = timeDelay;
        this.lowLimit = lowLimit;
        this.highLimit = highLimit;
        this.deadband = deadband;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, lowLimit, 1);
        write(queue, highLimit, 2);
        write(queue, deadband, 3);
    }

    public UnsignedOutOfRange(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        lowLimit = read(queue, UnsignedInteger.class, 1);
        highLimit = read(queue, UnsignedInteger.class, 2);
        deadband = read(queue, UnsignedInteger.class, 3);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public UnsignedInteger getLowLimit() {
        return lowLimit;
    }

    public UnsignedInteger getHighLimit() {
        return highLimit;
    }

    public UnsignedInteger getDeadband() {
        return deadband;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (deadband == null ? 0 : deadband.hashCode());
        result = prime * result + (highLimit == null ? 0 : highLimit.hashCode());
        result = prime * result + (lowLimit == null ? 0 : lowLimit.hashCode());
        result = prime * result + (timeDelay == null ? 0 : timeDelay.hashCode());
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
        final UnsignedOutOfRange other = (UnsignedOutOfRange) obj;
        if (deadband == null) {
            if (other.deadband != null)
                return false;
        } else if (!deadband.equals(other.deadband))
            return false;
        if (highLimit == null) {
            if (other.highLimit != null)
                return false;
        } else if (!highLimit.equals(other.highLimit))
            return false;
        if (lowLimit == null) {
            if (other.lowLimit != null)
                return false;
        } else if (!lowLimit.equals(other.lowLimit))
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
        return "UnsignedOutOfRange[ timeDelay=" + timeDelay + ", lowLimit=" + lowLimit + ", highLimit=" + highLimit + ", deadband=" + deadband + ']';
    }
}
