
package com.renovo.bacnet4j.util;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.PropertyReference;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class DeviceObjectPropertyValues {
    private final Map<Integer, PropertyValues> values = new HashMap<>();

    public DeviceObjectPropertyValues add(final int deviceId, final ObjectType objectType, final int objectNumber,
            final PropertyIdentifier pid, final int pin, final Encodable value) {
        return add(deviceId, new ObjectIdentifier(objectType, objectNumber), pid, new UnsignedInteger(pin), value);
    }

    public DeviceObjectPropertyValues add(final int deviceId, final ObjectType objectType, final int objectNumber,
            final PropertyIdentifier pid, final UnsignedInteger pin, final Encodable value) {
        return add(deviceId, new ObjectIdentifier(objectType, objectNumber), pid, pin, value);
    }

    public DeviceObjectPropertyValues add(final int deviceId, final ObjectIdentifier oid, final PropertyIdentifier pid,
            final UnsignedInteger pin, final Encodable value) {
        PropertyValues propertyValues = values.get(deviceId);
        if (propertyValues == null) {
            propertyValues = new PropertyValues();
            values.put(deviceId, propertyValues);
        }
        propertyValues.add(oid, pid, pin, value);
        return this;
    }

    public PropertyValues getPropertyValues(final int deviceId) {
        return values.get(deviceId);
    }

    //
    // Get properties the same way that they were put into DeviceObjectPropertyReferences
    //

    public Encodable get(final int deviceId, final ObjectType objectType, final int objectNumber,
            final PropertyIdentifier pid) {
        return get(deviceId, new ObjectIdentifier(objectType, objectNumber), pid);
    }

    public Encodable get(final int deviceId, final ObjectIdentifier oid, final PropertyIdentifier pid) {
        final PropertyValues propertyValues = getPropertyValues(deviceId);
        if (propertyValues == null) {
            return null;
        }
        return propertyValues.getNoErrorCheck(oid, pid);
    }

    public Encodable getIndex(final int deviceId, final ObjectType objectType, final int objectNumber,
            final PropertyIdentifier pid, final int pin) {
        return getIndex(deviceId, new ObjectIdentifier(objectType, objectNumber), pid, new UnsignedInteger(pin));
    }

    public Encodable getIndex(final int deviceId, final ObjectIdentifier oid, final PropertyIdentifier pid,
            final UnsignedInteger pin) {
        final PropertyValues propertyValues = getPropertyValues(deviceId);
        if (propertyValues == null) {
            return null;
        }
        return propertyValues.getNoErrorCheck(oid, new PropertyReference(pid, pin));
    }

    public int size() {
        int sum = 0;
        for (final PropertyValues pvs : values.values()) {
            sum += pvs.size();
        }
        return sum;
    }

    public void clear() {
        values.clear();
    }

    @Override
    public String toString() {
        return "DevicesObjectPropertyValues [values=" + values + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (values == null ? 0 : values.hashCode());
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
        final DeviceObjectPropertyValues other = (DeviceObjectPropertyValues) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}
