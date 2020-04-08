
package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AccessEvent extends AbstractEventParameter {
    public static final byte TYPE_ID = 13;

    private final SequenceOf<com.renovo.bacnet4j.type.enumerated.AccessEvent> listOfAccessEvents;
    private final DeviceObjectPropertyReference accessEventTimeReference;

    public AccessEvent(final SequenceOf<com.renovo.bacnet4j.type.enumerated.AccessEvent> listOfAccessEvents,
            final DeviceObjectPropertyReference accessEventTimeReference) {
        this.listOfAccessEvents = listOfAccessEvents;
        this.accessEventTimeReference = accessEventTimeReference;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, listOfAccessEvents, 0);
        write(queue, accessEventTimeReference, 1);
    }

    public AccessEvent(final ByteQueue queue) throws BACnetException {
        listOfAccessEvents = readSequenceOf(queue, com.renovo.bacnet4j.type.enumerated.AccessEvent.class, 0);
        accessEventTimeReference = read(queue, DeviceObjectPropertyReference.class, 1);
    }

    public SequenceOf<com.renovo.bacnet4j.type.enumerated.AccessEvent> getListOfAccessEvents() {
        return listOfAccessEvents;
    }

    public DeviceObjectPropertyReference getAccessEventTimeReference() {
        return accessEventTimeReference;
    }

    @Override
    public EventAlgorithm createEventAlgorithm() {
        return null;
    }

    @Override
    public DeviceObjectPropertyReference getReference() {
        return accessEventTimeReference;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (accessEventTimeReference == null ? 0 : accessEventTimeReference.hashCode());
        result = prime * result + (listOfAccessEvents == null ? 0 : listOfAccessEvents.hashCode());
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
        final AccessEvent other = (AccessEvent) obj;
        if (accessEventTimeReference == null) {
            if (other.accessEventTimeReference != null)
                return false;
        } else if (!accessEventTimeReference.equals(other.accessEventTimeReference))
            return false;
        if (listOfAccessEvents == null) {
            if (other.listOfAccessEvents != null)
                return false;
        } else if (!listOfAccessEvents.equals(other.listOfAccessEvents))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AccessEvent[ listOfAccessEvents=" + listOfAccessEvents + ", accessEventTimeReference=" + accessEventTimeReference + ']';
    }
}
