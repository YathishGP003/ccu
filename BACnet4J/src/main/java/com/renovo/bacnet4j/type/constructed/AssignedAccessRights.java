
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class AssignedAccessRights extends BaseType {
    private final DeviceObjectReference assignedAccessRights;
    private final Boolean enabled;

    public AssignedAccessRights(final DeviceObjectReference assignedAccessRights, final Boolean enabled) {
        this.assignedAccessRights = assignedAccessRights;
        this.enabled = enabled;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, assignedAccessRights);
        write(queue, enabled);
    }

    public AssignedAccessRights(final ByteQueue queue) throws BACnetException {
        assignedAccessRights = read(queue, DeviceObjectReference.class);
        enabled = read(queue, Boolean.class);
    }

    public DeviceObjectReference getAssignedAccessRights() {
        return assignedAccessRights;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (assignedAccessRights == null ? 0 : assignedAccessRights.hashCode());
        result = prime * result + (enabled == null ? 0 : enabled.hashCode());
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
        final AssignedAccessRights other = (AssignedAccessRights) obj;
        if (assignedAccessRights == null) {
            if (other.assignedAccessRights != null)
                return false;
        } else if (!assignedAccessRights.equals(other.assignedAccessRights))
            return false;
        if (enabled == null) {
            if (other.enabled != null)
                return false;
        } else if (!enabled.equals(other.enabled))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "AssignedAccessRights [assignedAccessRights=" + assignedAccessRights + ", enabled=" + enabled + ']';
    }   
}
