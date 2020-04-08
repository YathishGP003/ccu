
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.OutOfRangeAlgo;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class OutOfRange extends AbstractEventParameter {
    public static final byte TYPE_ID = 5;

    private final UnsignedInteger timeDelay;
    private final Real lowLimit;
    private final Real highLimit;
    private final Real deadband;

    public OutOfRange(final UnsignedInteger timeDelay, final Real lowLimit, final Real highLimit, final Real deadband) {
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

    public OutOfRange(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        lowLimit = read(queue, Real.class, 1);
        highLimit = read(queue, Real.class, 2);
        deadband = read(queue, Real.class, 3);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public Real getLowLimit() {
        return lowLimit;
    }

    public Real getHighLimit() {
        return highLimit;
    }

    public Real getDeadband() {
        return deadband;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new OutOfRangeAlgo();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deadband == null ? 0 : deadband.hashCode());
        result = PRIME * result + (highLimit == null ? 0 : highLimit.hashCode());
        result = PRIME * result + (lowLimit == null ? 0 : lowLimit.hashCode());
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
        final OutOfRange other = (OutOfRange) obj;
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
        return "OutOfRange[ timeDelay=" + timeDelay + ", lowLimit=" + lowLimit + ", highLimit=" + highLimit + ", deadband=" + deadband + ']';
    }
}
