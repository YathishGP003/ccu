
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.ChangeOfStateAlgo;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.PropertyStates;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfState extends AbstractEventParameter {
    public static final byte TYPE_ID = 1;

    private final UnsignedInteger timeDelay;
    private final SequenceOf<PropertyStates> listOfValues;

    public ChangeOfState(final UnsignedInteger timeDelay, final SequenceOf<PropertyStates> listOfValues) {
        this.timeDelay = timeDelay;
        this.listOfValues = listOfValues;
    }

    public ChangeOfState(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        listOfValues = readSequenceOf(queue, PropertyStates.class, 1);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, listOfValues, 1);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public SequenceOf<PropertyStates> getListOfValues() {
        return listOfValues;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return new ChangeOfStateAlgo();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfValues == null ? 0 : listOfValues.hashCode());
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
        final ChangeOfState other = (ChangeOfState) obj;
        if (listOfValues == null) {
            if (other.listOfValues != null)
                return false;
        } else if (!listOfValues.equals(other.listOfValues))
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
        return "ChangeOfState[ timeDelay=" + timeDelay + ", listOfValues=" + listOfValues + ']';
    }  
}
