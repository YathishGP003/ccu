
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class DeviceObjectReference extends BaseType {
    private final ObjectIdentifier deviceIdentifier;
    private final ObjectIdentifier objectIdentifier;

    public DeviceObjectReference(final ObjectIdentifier deviceIdentifier, final ObjectIdentifier objectIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
        this.objectIdentifier = objectIdentifier;
    }

    @Override
    public void write(final ByteQueue queue) {
        writeOptional(queue, deviceIdentifier, 0);
        write(queue, objectIdentifier, 1);
    }

    public DeviceObjectReference(final ByteQueue queue) throws BACnetException {
        deviceIdentifier = readOptional(queue, ObjectIdentifier.class, 0);
        objectIdentifier = read(queue, ObjectIdentifier.class, 1);
    }

    public ObjectIdentifier getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deviceIdentifier == null ? 0 : deviceIdentifier.hashCode());
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
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
        final DeviceObjectReference other = (DeviceObjectReference) obj;
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
        return true;
    }

    @Override
    public String toString() {
        return "DeviceObjectReference [deviceIdentifier=" + deviceIdentifier + ", objectIdentifier=" + objectIdentifier + ']';
    }
}
