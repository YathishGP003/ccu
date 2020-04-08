
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AddressBinding extends BaseType {
    private final ObjectIdentifier deviceObjectIdentifier;
    private final Address deviceAddress;

    public AddressBinding(final ObjectIdentifier deviceObjectIdentifier, final Address deviceAddress) {
        this.deviceObjectIdentifier = deviceObjectIdentifier;
        this.deviceAddress = deviceAddress;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, deviceObjectIdentifier);
        write(queue, deviceAddress);
    }

    public AddressBinding(final ByteQueue queue) throws BACnetException {
        deviceObjectIdentifier = read(queue, ObjectIdentifier.class);
        deviceAddress = read(queue, Address.class);
    }

    public ObjectIdentifier getDeviceObjectIdentifier() {
        return deviceObjectIdentifier;
    }

    public Address getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public String toString() {
        return "AddressBinding [deviceObjectIdentifier=" + deviceObjectIdentifier + ", deviceAddress=" + deviceAddress
                + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (deviceAddress == null ? 0 : deviceAddress.hashCode());
        result = PRIME * result + (deviceObjectIdentifier == null ? 0 : deviceObjectIdentifier.hashCode());
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
        final AddressBinding other = (AddressBinding) obj;
        if (deviceAddress == null) {
            if (other.deviceAddress != null)
                return false;
        } else if (!deviceAddress.equals(other.deviceAddress))
            return false;
        if (deviceObjectIdentifier == null) {
            if (other.deviceObjectIdentifier != null)
                return false;
        } else if (!deviceObjectIdentifier.equals(other.deviceObjectIdentifier))
            return false;
        return true;
    }
}
