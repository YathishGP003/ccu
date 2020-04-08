
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.OctetString;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class VmacEntry extends BaseType {
    private final OctetString virtualMacAddress;
    private final OctetString nativeMacAddress;

    public VmacEntry(final OctetString virtualMacAddress, final OctetString nativeMacAddress) {
        this.virtualMacAddress = virtualMacAddress;
        this.nativeMacAddress = nativeMacAddress;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, virtualMacAddress, 0);
        write(queue, nativeMacAddress, 1);
    }

    public VmacEntry(final ByteQueue queue) throws BACnetException {
        virtualMacAddress = read(queue, OctetString.class, 0);
        nativeMacAddress = read(queue, OctetString.class, 1);
    }

    public OctetString getVirtualMacAddress() {
        return virtualMacAddress;
    }

    public OctetString getNativeMacAddress() {
        return nativeMacAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (nativeMacAddress == null ? 0 : nativeMacAddress.hashCode());
        result = prime * result + (virtualMacAddress == null ? 0 : virtualMacAddress.hashCode());
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
        final VmacEntry other = (VmacEntry) obj;
        if (nativeMacAddress == null) {
            if (other.nativeMacAddress != null)
                return false;
        } else if (!nativeMacAddress.equals(other.nativeMacAddress))
            return false;
        if (virtualMacAddress == null) {
            if (other.virtualMacAddress != null)
                return false;
        } else if (!virtualMacAddress.equals(other.virtualMacAddress))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "VmacEntry [virtualMacAddress=" + virtualMacAddress + ", nativeMacAddress=" + nativeMacAddress + ']';
    }
}
