
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.enumerated.TimerState;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ChangeOfTimer extends AbstractEventParameter {
    public static final byte TYPE_ID = 22;

    private final UnsignedInteger timeDelay;
    private final SequenceOf<TimerState> alarmValues;
    private final DeviceObjectPropertyReference updateTimeReference;

    public ChangeOfTimer(final UnsignedInteger timeDelay, final SequenceOf<TimerState> alarmValues,
            final DeviceObjectPropertyReference updateTimeReference) {
        this.timeDelay = timeDelay;
        this.alarmValues = alarmValues;
        this.updateTimeReference = updateTimeReference;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, timeDelay, 0);
        write(queue, alarmValues, 1);
        write(queue, updateTimeReference, 2);
    }

    public ChangeOfTimer(final ByteQueue queue) throws BACnetException {
        timeDelay = read(queue, UnsignedInteger.class, 0);
        alarmValues = readSequenceOf(queue, TimerState.class, 1);
        updateTimeReference = read(queue, DeviceObjectPropertyReference.class, 2);
    }

    public UnsignedInteger getTimeDelay() {
        return timeDelay;
    }

    public SequenceOf<TimerState> getAlarmValues() {
        return alarmValues;
    }

    public DeviceObjectPropertyReference getUpdateTimeReference() {
        return updateTimeReference;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public DeviceObjectPropertyReference getReference() {
        return updateTimeReference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (alarmValues == null ? 0 : alarmValues.hashCode());
        result = prime * result + (timeDelay == null ? 0 : timeDelay.hashCode());
        result = prime * result + (updateTimeReference == null ? 0 : updateTimeReference.hashCode());
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
        final ChangeOfTimer other = (ChangeOfTimer) obj;
        if (alarmValues == null) {
            if (other.alarmValues != null)
                return false;
        } else if (!alarmValues.equals(other.alarmValues))
            return false;
        if (timeDelay == null) {
            if (other.timeDelay != null)
                return false;
        } else if (!timeDelay.equals(other.timeDelay))
            return false;
        if (updateTimeReference == null) {
            if (other.updateTimeReference != null)
                return false;
        } else if (!updateTimeReference.equals(other.updateTimeReference))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ChangeOfTimer[ timeDelay=" + timeDelay + ", alarmValues=" + alarmValues + ", updateTimeReference=" + updateTimeReference + ']';
    }
}
