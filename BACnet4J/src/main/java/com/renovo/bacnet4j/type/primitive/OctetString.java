
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import java.util.Arrays;

import com.renovo.bacnet4j.npdu.NetworkUtils;
import com.renovo.bacnet4j.util.sero.ArrayUtils;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class OctetString extends Primitive {
    public static final byte TYPE_ID = 6;

    private final byte[] value;

    public OctetString(final byte[] value) {
        this.value = value;
    }

    public byte[] getBytes() {
        return value;
    }

    //
    // Reading and writing
    //
    public OctetString(final ByteQueue queue) throws BACnetErrorException {
        final int length = (int) readTag(queue, TYPE_ID);
        value = new byte[length];
        queue.pop(value);
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        queue.push(value);
    }

    @Override
    public long getLength() {
        return value.length;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + Arrays.hashCode(value);
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
        final OctetString other = (OctetString) obj;
        if (!Arrays.equals(value, other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return ArrayUtils.toHexString(value);
    }

    public String getDescription() {
        return NetworkUtils.toString(this);
    }
}
