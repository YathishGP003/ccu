
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.CommandFailureAlgo;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class CommandFailure extends AbstractEventParameter {
    public static final byte TYPE_ID = 3;

    private final UnsignedInteger timeDelay;
    private final DeviceObjectPropertyReference feedbackPropertyReference;

    public CommandFailure(final UnsignedInteger timeDelay,
            final DeviceObjectPropertyReference feedbackPropertyReference) {
        this.timeDelay = timeDelay;
        this.feedbackPropertyReference = feedbackPropertyReference;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, feedbackPropertyReference, 1);
    }

    public CommandFailure(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        feedbackPropertyReference = read(queue, DeviceObjectPropertyReference.class, 1);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public DeviceObjectPropertyReference getFeedbackPropertyReference() {
        return feedbackPropertyReference;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new CommandFailureAlgo();
    }

    @Override
    public DeviceObjectPropertyReference getReference() {
        return feedbackPropertyReference;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (feedbackPropertyReference == null ? 0 : feedbackPropertyReference.hashCode());
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
        final CommandFailure other = (CommandFailure) obj;
        if (feedbackPropertyReference == null) {
            if (other.feedbackPropertyReference != null)
                return false;
        } else if (!feedbackPropertyReference.equals(other.feedbackPropertyReference))
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
        return "CommandFailure[ timeDelay=" + timeDelay + ", feedbackPropertyReference=" + feedbackPropertyReference + ']';
    }
}
