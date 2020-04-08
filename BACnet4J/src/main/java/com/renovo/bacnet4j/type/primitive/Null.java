
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Null extends Primitive {
    public static final Null instance = new Null();

    public static final byte TYPE_ID = 0;

    public Null() {
        // no op
    }

    public Null(final ByteQueue queue) throws BACnetErrorException {
        readTag(queue, TYPE_ID);
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        // no op
    }

    @Override
    protected long getLength() {
        return 0;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Null";
    }
}
