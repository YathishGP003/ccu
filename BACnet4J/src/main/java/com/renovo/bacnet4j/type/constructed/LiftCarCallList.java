
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Unsigned8;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class LiftCarCallList extends BaseType {
    private final SequenceOf<Unsigned8> floorNumbers;

    public LiftCarCallList(final SequenceOf<Unsigned8> floorNumbers) {
        this.floorNumbers = floorNumbers;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, floorNumbers, 0);
    }

    public LiftCarCallList(final ByteQueue queue) throws BACnetException {
        floorNumbers = readSequenceOf(queue, Unsigned8.class, 0);
    }

    public SequenceOf<Unsigned8> getFloorNumbers() {
        return floorNumbers;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (floorNumbers == null ? 0 : floorNumbers.hashCode());
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
        final LiftCarCallList other = (LiftCarCallList) obj;
        if (floorNumbers == null) {
            if (other.floorNumbers != null)
                return false;
        } else if (!floorNumbers.equals(other.floorNumbers))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "LiftCarCallList[" + "floorNumbers=" + floorNumbers + ']';
    }    
}
