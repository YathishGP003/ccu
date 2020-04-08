
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class DeviceObjectPropertyValue extends BaseType {
    private final ObjectIdentifier deviceIdentifier;
    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger arrayIndex;
    private final Encodable value;

    public DeviceObjectPropertyValue(final ObjectIdentifier deviceIdentifier, final ObjectIdentifier objectIdentifier,
            final PropertyIdentifier propertyIdentifier, final UnsignedInteger arrayIndex, final Encodable value) {
        this.deviceIdentifier = deviceIdentifier;
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.arrayIndex = arrayIndex;
        this.value = value;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, deviceIdentifier, 0);
        write(queue, objectIdentifier, 1);
        write(queue, propertyIdentifier, 2);
        writeOptional(queue, arrayIndex, 3);
        write(queue, value, 4);
    }

    public DeviceObjectPropertyValue(final ByteQueue queue) throws BACnetException {
        deviceIdentifier = read(queue, ObjectIdentifier.class, 0);
        objectIdentifier = read(queue, ObjectIdentifier.class, 1);
        propertyIdentifier = read(queue, PropertyIdentifier.class, 2);
        arrayIndex = readOptional(queue, UnsignedInteger.class, 3);
        value = readANY(queue, objectIdentifier.getObjectType(), propertyIdentifier, null, 4);
    }

    public ObjectIdentifier getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public UnsignedInteger getArrayIndex() {
        return arrayIndex;
    }

    public Encodable getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (arrayIndex == null ? 0 : arrayIndex.hashCode());
        result = PRIME * result + (deviceIdentifier == null ? 0 : deviceIdentifier.hashCode());
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
        result = PRIME * result + (value == null ? 0 : value.hashCode());
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
        final DeviceObjectPropertyValue other = (DeviceObjectPropertyValue) obj;
        if (arrayIndex == null) {
            if (other.arrayIndex != null)
                return false;
        } else if (!arrayIndex.equals(other.arrayIndex))
            return false;
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
        if (propertyIdentifier == null) {
            if (other.propertyIdentifier != null)
                return false;
        } else if (!propertyIdentifier.equals(other.propertyIdentifier))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DeviceObjectPropertyValue [deviceIdentifier=" + deviceIdentifier + ", objectIdentifier=" + objectIdentifier + ", propertyIdentifier=" + propertyIdentifier + ", arrayIndex=" + arrayIndex + ", value=" + value + ']';
    }  
}
