
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.AmbiguousValue;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class PropertyAccessResult extends BaseType {
    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;
    private final ObjectIdentifier deviceIdentifier;
    private final Encodable result;

    public PropertyAccessResult(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex, final ObjectIdentifier deviceIdentifier, final Encodable result) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.deviceIdentifier = deviceIdentifier;
        this.result = result;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public UnsignedInteger getPropertyArrayIndex() {
        return propertyArrayIndex;
    }

    public ObjectIdentifier getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public Encodable getResult() {
        return result;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
        writeOptional(queue, deviceIdentifier, 3);
        if (result instanceof ErrorClassAndCode)
            write(queue, result, 4);
        else
            write(queue, result, 5);
    }

    public PropertyAccessResult(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
        propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
        deviceIdentifier = readOptional(queue, ObjectIdentifier.class, 3);

        final Encodable result = readOptional(queue, ErrorClassAndCode.class, 4);
        if (result == null)
            this.result = result;
        else
            this.result = new AmbiguousValue(queue, 5);
    }

    @Override
    public String toString() {
        return "PropertyAccessResult [objectIdentifier=" + objectIdentifier + ", propertyIdentifier="
                + propertyIdentifier + ", propertyArrayIndex=" + propertyArrayIndex + ", deviceIdentifier="
                + deviceIdentifier + ", result=" + result + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (deviceIdentifier == null ? 0 : deviceIdentifier.hashCode());
        result = prime * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = prime * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = prime * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
        result = prime * result + (this.result == null ? 0 : this.result.hashCode());
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
        final PropertyAccessResult other = (PropertyAccessResult) obj;
        if (deviceIdentifier == null) {
            if (other.deviceIdentifier != null)
                return false;
        } else if (!deviceIdentifier.equals(other.deviceIdentifier))
            return false;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        if (propertyArrayIndex == null) {
            if (other.propertyArrayIndex != null)
                return false;
        } else if (!propertyArrayIndex.equals(other.propertyArrayIndex))
            return false;
        if (propertyIdentifier == null) {
            if (other.propertyIdentifier != null)
                return false;
        } else if (!propertyIdentifier.equals(other.propertyIdentifier))
            return false;
        if (result == null) {
            if (other.result != null)
                return false;
        } else if (!result.equals(other.result))
            return false;
        return true;
    }
}
