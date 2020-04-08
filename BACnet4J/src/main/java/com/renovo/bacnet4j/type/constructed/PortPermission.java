
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.Unsigned8;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class PortPermission extends BaseType {
    private final Unsigned8 portId;
    private final Boolean enabled;

    public PortPermission(final Unsigned8 portId, final Boolean enabled) {
        this.portId = portId;
        this.enabled = enabled;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, portId, 0);
        write(queue, enabled, 1);
    }

    public PortPermission(final ByteQueue queue) throws BACnetException {
        portId = read(queue, Unsigned8.class, 0);
        enabled = read(queue, Boolean.class, 1);
    }

    public Unsigned8 getPortId() {
        return portId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "PortPermission [portId=" + portId + ", enabled=" + enabled + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (enabled == null ? 0 : enabled.hashCode());
        result = prime * result + (portId == null ? 0 : portId.hashCode());
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
        final PortPermission other = (PortPermission) obj;
        if (enabled == null) {
            if (other.enabled != null)
                return false;
        } else if (!enabled.equals(other.enabled))
            return false;
        if (portId == null) {
            if (other.portId != null)
                return false;
        } else if (!portId.equals(other.portId))
            return false;
        return true;
    }
}
