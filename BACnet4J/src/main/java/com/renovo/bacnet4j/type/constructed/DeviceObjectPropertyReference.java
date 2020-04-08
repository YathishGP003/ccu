
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class DeviceObjectPropertyReference extends BaseType {
    private final ObjectIdentifier objectIdentifier; // 0
    private final PropertyIdentifier propertyIdentifier; // 1
    private final UnsignedInteger propertyArrayIndex; // 2 optional
    private final ObjectIdentifier deviceIdentifier; // 3 optional

    public DeviceObjectPropertyReference(final int deviceNumber, final ObjectIdentifier objectIdentifier,
            final PropertyIdentifier propertyIdentifier) {
        this(objectIdentifier, propertyIdentifier, null, new ObjectIdentifier(ObjectType.device, deviceNumber));
    }

    public DeviceObjectPropertyReference(final ObjectIdentifier objectIdentifier,
            final PropertyIdentifier propertyIdentifier, final UnsignedInteger propertyArrayIndex,
            final ObjectIdentifier deviceIdentifier) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.deviceIdentifier = deviceIdentifier;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
        writeOptional(queue, deviceIdentifier, 3);
    }

    public DeviceObjectPropertyReference(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
        propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
        deviceIdentifier = readOptional(queue, ObjectIdentifier.class, 3);
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

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deviceIdentifier == null ? 0 : deviceIdentifier.hashCode());
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
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
        final DeviceObjectPropertyReference other = (DeviceObjectPropertyReference) obj;
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
        return true;
    }

    @Override
    public String toString() {
        return "DeviceObjectPropertyReference [objectIdentifier=" + objectIdentifier + ", propertyIdentifier="
                + propertyIdentifier + ", propertyArrayIndex=" + propertyArrayIndex + ", deviceIdentifier="
                + deviceIdentifier + "]";
    }
}
