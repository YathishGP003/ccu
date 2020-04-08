
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfCharacterString extends AbstractEventParameter {
    public static final byte TYPE_ID = 17;

    private final UnsignedInteger timeDelay;
    private final SequenceOf<CharacterString> listOfAlarmValues;

    public ChangeOfCharacterString(final UnsignedInteger timeDelay,
            final SequenceOf<CharacterString> listOfAlarmValues) {
        this.timeDelay = timeDelay;
        this.listOfAlarmValues = listOfAlarmValues;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, listOfAlarmValues, 1);
    }

    public ChangeOfCharacterString(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        listOfAlarmValues = readSequenceOf(queue, CharacterString.class, 1);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public SequenceOf<CharacterString> getListOfAlarmValues() {
        return listOfAlarmValues;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (listOfAlarmValues == null ? 0 : listOfAlarmValues.hashCode());
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
        final ChangeOfCharacterString other = (ChangeOfCharacterString) obj;
        if (listOfAlarmValues == null) {
            if (other.listOfAlarmValues != null)
                return false;
        } else if (!listOfAlarmValues.equals(other.listOfAlarmValues))
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
        return "ChangeOfCharacterString[ timeDelay=" + timeDelay + ", listOfAlarmValues=" + listOfAlarmValues + ']';
    }    
}
