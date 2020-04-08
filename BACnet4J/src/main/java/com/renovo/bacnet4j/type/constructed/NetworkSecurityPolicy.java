
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.SecurityPolicy;
import com.renovo.bacnet4j.type.primitive.Unsigned8;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class NetworkSecurityPolicy extends BaseType {
    private final Unsigned8 portId;
    private final SecurityPolicy securityLevel;

    public NetworkSecurityPolicy(final Unsigned8 portId, final SecurityPolicy securityLevel) {
        this.portId = portId;
        this.securityLevel = securityLevel;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, portId, 0);
        write(queue, securityLevel, 1);
    }

    public NetworkSecurityPolicy(final ByteQueue queue) throws BACnetException {
        portId = read(queue, Unsigned8.class, 0);
        securityLevel = read(queue, SecurityPolicy.class, 1);
    }

    public Unsigned8 getPortId() {
        return portId;
    }

    public SecurityPolicy getSecurityLevel() {
        return securityLevel;
    }

    @Override
    public String toString() {
        return "NetworkSecurityPolicy [portId=" + portId + ", securityLevel=" + securityLevel + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (portId == null ? 0 : portId.hashCode());
        result = prime * result + (securityLevel == null ? 0 : securityLevel.hashCode());
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
        final NetworkSecurityPolicy other = (NetworkSecurityPolicy) obj;
        if (portId == null) {
            if (other.portId != null)
                return false;
        } else if (!portId.equals(other.portId))
            return false;
        if (securityLevel == null) {
            if (other.securityLevel != null)
                return false;
        } else if (!securityLevel.equals(other.securityLevel))
            return false;
        return true;
    }
}
