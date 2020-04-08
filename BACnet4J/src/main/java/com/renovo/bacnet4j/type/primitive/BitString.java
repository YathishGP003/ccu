
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import java.util.Arrays;

import com.renovo.bacnet4j.util.BACnetUtils;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class BitString extends Primitive {
    public static final byte TYPE_ID = 8;

    private boolean[] value;

    public BitString(final boolean[] value) {
        this.value = value;
    }

    public BitString(final int size, final boolean defaultValue) {
        value = new boolean[size];
        if (defaultValue) {
            for (int i = 0; i < size; i++)
                value[i] = true;
        }
    }

    public BitString(final BitString that) {
        this(Arrays.copyOf(that.value, that.value.length));
    }

    public boolean[] getValue() {
        return value;
    }

    public boolean getValue(final int indexBase1) {
        return value[indexBase1 - 1];
    }

    public boolean getArrayValue(final int index) {
        final boolean[] ba = getValue();
        if (index < ba.length)
            return ba[index];
        return false;
    }

    public BitString setAll(final boolean b) {
        for (int i = 0; i < value.length; i++)
            value[i] = b;
        return this;
    }

    public void setValue(final int indexBase1, final boolean b) {
        value[indexBase1 - 1] = b;
    }

    public boolean allFalse() {
        for (int i = 0; i < value.length; i++) {
            if (value[i])
                return false;
        }
        return true;
    }

    public boolean allTrue() {
        for (int i = 0; i < value.length; i++) {
            if (!value[i])
                return false;
        }
        return true;
    }

    /**
     * Performs a bit-wise AND operation.
     */
    public BitString and(final BitString that) {
        if (value.length != that.value.length)
            throw new IllegalArgumentException("Bitstrings are of different lengths");

        final boolean[] result = new boolean[value.length];
        for (int i = 0; i < value.length; i++) {
            result[i] = value[i] && that.value[i];
        }
        return new BitString(result);
    }

    //
    // Reading and writing
    //
    public BitString(final ByteQueue queue) throws BACnetErrorException {
        final int length = (int) readTag(queue, TYPE_ID) - 1;
        final int remainder = queue.popU1B();

        if (length == 0)
            value = new boolean[0];
        else {
            final byte[] data = new byte[length];
            queue.pop(data);
            value = BACnetUtils.convertToBooleans(data, length * 8 - remainder);
        }
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        if (value.length == 0)
            queue.push((byte) 0);
        else {
            int remainder = value.length % 8;
            if (remainder > 0)
                remainder = 8 - remainder;
            queue.push((byte) remainder);
            queue.push(BACnetUtils.convertToBytes(value));
        }
    }

    @Override
    protected long getLength() {
        if (value.length == 0)
            return 1;
        return (value.length - 1) / 8 + 2;
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
        final BitString other = (BitString) obj;
        if (!Arrays.equals(value, other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return Arrays.toString(value);
    }
}
