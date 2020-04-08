
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.primitive.BitString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class EventTransitionBits extends BitString {
    public EventTransitionBits(final boolean toOffnormal, final boolean toFault, final boolean toNormal) {
        super(new boolean[] { toOffnormal, toFault, toNormal });
    }

    public EventTransitionBits(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    public EventTransitionBits(final EventTransitionBits that) {
        super(that);
    }

    public boolean isToOffnormal() {
        return getValue()[0];
    }

    public boolean isToFault() {
        return getValue()[1];
    }

    public boolean isToNormal() {
        return getValue()[2];
    }

    public boolean contains(final EventState toState) {
        return getValue(toState.getTransitionIndex());
    }

    @Override
    public String toString() {
        return "EventTransitionBits [to-offnormal=" + isToOffnormal() + ", to-fault=" + isToFault() + ", to-normal=" + isToNormal() + "]";
    }
}
