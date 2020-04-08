
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.util.BACnetUtils;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Double extends Primitive {
    public static final byte TYPE_ID = 5;

    private final double value;

    public Double(final double value) {
        this.value = value;
    }

    public double doubleValue() {
        return value;
    }

    //
    // Reading and writing
    //
    public Double(final ByteQueue queue) throws BACnetErrorException {
        readTag(queue, TYPE_ID);
        value = java.lang.Double.longBitsToDouble(BACnetUtils.popLong(queue));
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        BACnetUtils.pushLong(queue, java.lang.Double.doubleToLongBits(value));
    }

    @Override
    protected long getLength() {
        return 8;
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        long temp;
        temp = java.lang.Double.doubleToLongBits(value);
        result = PRIME * result + (int) (temp ^ temp >>> 32);
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
        final Double other = (Double) obj;
        if (java.lang.Double.doubleToLongBits(value) != java.lang.Double.doubleToLongBits(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return java.lang.Double.toString(value);
    }
}
