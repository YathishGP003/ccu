
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.util.BACnetUtils;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Real extends Primitive {
    public static final byte TYPE_ID = 4;

    private final float value;

    public Real(final float value) {
        this.value = value;
    }

    public float floatValue() {
        return value;
    }

    //
    // Reading and writing
    //
    public Real(final ByteQueue queue) throws BACnetErrorException {
        readTag(queue, TYPE_ID);
        value = Float.intBitsToFloat(BACnetUtils.popInt(queue));
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        BACnetUtils.pushInt(queue, Float.floatToIntBits(value));
    }

    @Override
    protected long getLength() {
        return 4;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + Float.floatToIntBits(value);
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
        final Real other = (Real) obj;
        if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Float.toString(value);
    }
}
