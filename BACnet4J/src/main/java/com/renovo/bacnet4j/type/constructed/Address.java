
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.type.primitive.Unsigned16;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class Address extends BaseType {
    public static final int LOCAL_NETWORK = 0;
    public static final int ALL_NETWORKS = 0xFFFF;
    public static final Address GLOBAL = new Address(new Unsigned16(ALL_NETWORKS), null);

    private final Unsigned16 networkNumber;
    private final OctetString macAddress;

    public Address(final byte[] macAddress) {
        this(new Unsigned16(LOCAL_NETWORK), new OctetString(macAddress));
    }

    public Address(final int networkNumber, final byte[] macAddress) {
        this(new Unsigned16(networkNumber), new OctetString(macAddress));
    }

    public Address(final OctetString macAddress) {
        this(new Unsigned16(LOCAL_NETWORK), macAddress);
    }

    public Address(final int networkNumber, final OctetString macAddress) {
        this(new Unsigned16(networkNumber), macAddress);
    }

    public Address(final Unsigned16 networkNumber, final OctetString macAddress) {
        this.networkNumber = networkNumber;
        this.macAddress = macAddress;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, networkNumber);
        write(queue, macAddress);
    }

    public Address(final ByteQueue queue) throws BACnetException {
        networkNumber = read(queue, Unsigned16.class);
        macAddress = read(queue, OctetString.class);
    }

    public OctetString getMacAddress() {
        return macAddress;
    }

    public UnsignedInteger getNetworkNumber() {
        return networkNumber;
    }

    public boolean isGlobal() {
        return networkNumber.intValue() == 0xFFFF;
    }

    //
    //
    // General convenience
    //
    public String getDescription() {
        final StringBuilder sb = new StringBuilder();
        sb.append(macAddress.getDescription());
        if (networkNumber.intValue() != 0)
            sb.append('(').append(networkNumber).append(')');
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Address [networkNumber=" + networkNumber + ", macAddress=" + macAddress + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (macAddress == null ? 0 : macAddress.hashCode());
        result = PRIME * result + (networkNumber == null ? 0 : networkNumber.hashCode());
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
        final Address other = (Address) obj;
        if (macAddress == null) {
            if (other.macAddress != null)
                return false;
        } else if (!macAddress.equals(other.macAddress))
            return false;
        if (networkNumber == null) {
            if (other.networkNumber != null)
                return false;
        } else if (!networkNumber.equals(other.networkNumber))
            return false;
        return true;
    }
}
