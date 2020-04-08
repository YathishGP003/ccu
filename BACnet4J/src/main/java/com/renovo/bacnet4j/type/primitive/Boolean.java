
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Boolean extends Primitive {
    public static final Boolean FALSE = new Boolean(false);
    public static final Boolean TRUE = new Boolean(true);

    public static Boolean valueOf(final boolean b) {
        return b ? TRUE : FALSE;
    }

    public static boolean falsey(final Boolean b) {
        return b == null || !b.booleanValue();
    }

    public static boolean truthy(final Boolean b) {
        return !falsey(b);
    }

    public static final byte TYPE_ID = 1;

    protected final boolean value;

    private Boolean(final boolean value) {
        this.value = value;
    }

    public boolean booleanValue() {
        return value;
    }

    public Boolean(final ByteQueue queue) throws BACnetErrorException {
        final byte b = queue.pop();
        int tagNumber = (b & 0xff) >> 4;
        final boolean contextSpecific = (b & 8) != 0;
        long length = b & 7;

        if (tagNumber == 0xf)
            // Extended tag.
            tagNumber = queue.popU1B();

        if (length == 5) {
            length = queue.popU1B();
            if (length == 254)
                length = queue.popU2B();
            else if (length == 255)
                length = queue.popU4B();
        }

        if (contextSpecific) {
            value = queue.pop() == 1;
        } else {
            //if the tagNumber its not contextSpecific, validate the type
            if (tagNumber != TYPE_ID) {
                throw new BACnetErrorException(ErrorClass.property, ErrorCode.invalidDataType);
            }
            value = length == 1;
        }
    }

    @Override
    public void write(final ByteQueue queue) {
        writeTag(queue, getTypeId(), false, value ? 1 : 0);
    }

    @Override
    public void write(final ByteQueue queue, final int contextId) {
        writeTag(queue, contextId, true, 1);
        queue.push((byte) (value ? 1 : 0));
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        throw new RuntimeException("Should not be called because length is context specific");
    }

    @Override
    protected long getLength() {
        throw new RuntimeException("Should not be called because length is context specific");
    }

    @Override
    public byte getTypeId() {
        return TYPE_ID;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (value ? 1231 : 1237);
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
        final Boolean other = (Boolean) obj;
        if (value != other.value)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return java.lang.Boolean.toString(value);
    }
}
