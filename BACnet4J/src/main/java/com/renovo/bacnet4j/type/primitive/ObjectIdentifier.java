
package com.renovo.bacnet4j.type.primitive;

import com.renovo.bacnet4j.exception.BACnetErrorException;
import java.util.Objects;

import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ObjectIdentifier extends Primitive {
    public static final int UNINITIALIZED = 4194303;
    public static final byte TYPE_ID = 12;

    private final ObjectType objectType;
    private int instanceNumber;

    public ObjectIdentifier(final int objectType, final int instanceNumber) {
        this(ObjectType.forId(objectType), instanceNumber);
    }

    public ObjectIdentifier(final ObjectType objectType, final int instanceNumber) {
        Objects.requireNonNull(objectType);

        if (instanceNumber < 0 || instanceNumber > 0x3FFFFF)
            throw new IllegalArgumentException("Illegal instance number: " + instanceNumber);

        this.objectType = objectType;
        this.instanceNumber = instanceNumber;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public int getInstanceNumber() {
        return instanceNumber;
    }

    public boolean isUninitialized() {
        return instanceNumber == UNINITIALIZED;
    }

    @Override
    public String toString() {
        return objectType.toString() + " " + instanceNumber;
    }

    //
    // Reading and writing
    //
    public ObjectIdentifier(final ByteQueue queue) throws BACnetErrorException {
        readTag(queue, TYPE_ID);

        int objectType = queue.popU1B() << 2;
        final int i = queue.popU1B();
        objectType |= i >> 6;

        this.objectType = ObjectType.forId(objectType);

        instanceNumber = (i & 0x3f) << 16;
        instanceNumber |= queue.popU1B() << 8;
        instanceNumber |= queue.popU1B();
    }

    @Override
    public void writeImpl(final ByteQueue queue) {
        final int objectType = this.objectType.intValue();
        queue.push(objectType >> 2);
        queue.push((objectType & 3) << 6 | instanceNumber >> 16);
        queue.push(instanceNumber >> 8);
        queue.push(instanceNumber);
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
        result = PRIME * result + instanceNumber;
        result = PRIME * result + (objectType == null ? 0 : objectType.hashCode());
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
        final ObjectIdentifier other = (ObjectIdentifier) obj;
        if (instanceNumber != other.instanceNumber)
            return false;
        if (objectType == null) {
            if (other.objectType != null)
                return false;
        } else if (!objectType.equals(other.objectType))
            return false;
        return true;
    }
}
