
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class SetpointReference extends BaseType {
    private final ObjectPropertyReference setpointReference;

    public SetpointReference(final ObjectPropertyReference setpointReference) {
        this.setpointReference = setpointReference;
    }

    @Override
    public void write(final ByteQueue queue) {
        writeOptional(queue, setpointReference, 0);
    }

    public SetpointReference(final ByteQueue queue) throws BACnetException {
        setpointReference = readOptional(queue, ObjectPropertyReference.class, 0);
    }

    public ObjectPropertyReference getSetpointReference() {
        return setpointReference;
    }

    @Override
    public String toString() {
        return "SetpointReference(setpointReference=" + setpointReference + ")";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (setpointReference == null ? 0 : setpointReference.hashCode());
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
        final SetpointReference other = (SetpointReference) obj;
        if (setpointReference == null) {
            if (other.setpointReference != null)
                return false;
        } else if (!setpointReference.equals(other.setpointReference))
            return false;
        return true;
    }
}
