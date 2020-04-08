
package com.renovo.bacnet4j.util;

import java.util.LinkedHashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class DeviceObjectPropertyReferences {
    private final Map<Integer, PropertyReferences> properties = new LinkedHashMap<>();

    public DeviceObjectPropertyReferences add(final int deviceId, final ObjectType objectType, final int objectNumber,
            final PropertyIdentifier... pids) {
        return add(deviceId, new ObjectIdentifier(objectType, objectNumber), pids);
    }

    public DeviceObjectPropertyReferences add(final int deviceId, final ObjectIdentifier oid,
            final PropertyIdentifier... pids) {
        final PropertyReferences refs = getDeviceProperties(deviceId);
        for (final PropertyIdentifier pid : pids)
            refs.add(oid, pid);
        return this;
    }

    public DeviceObjectPropertyReferences addIndex(final int deviceId, final ObjectType objectType,
            final int objectNumber, final PropertyIdentifier pid, final int pin) {
        return addIndex(deviceId, objectType, objectNumber, pid, new UnsignedInteger(pin));
    }

    public DeviceObjectPropertyReferences addIndex(final int deviceId, final ObjectType objectType,
            final int objectNumber, final PropertyIdentifier pid, final UnsignedInteger pin) {
        return addIndex(deviceId, new ObjectIdentifier(objectType, objectNumber), pid, pin);
    }

    public DeviceObjectPropertyReferences addIndex(final int deviceId, final ObjectIdentifier oid,
            final PropertyIdentifier pid, final UnsignedInteger pin) {
        final PropertyReferences refs = getDeviceProperties(deviceId);
        refs.addIndex(oid, pid, pin);
        return this;
    }

    public DeviceObjectPropertyReferences add(final int deviceId, final PropertyReferences refs) {
        final PropertyReferences existing = properties.get(deviceId);
        if (existing == null)
            properties.put(deviceId, refs);
        else
            existing.add(refs);
        return this;
    }

    public PropertyReferences getDeviceProperties(final Integer deviceId) {
        PropertyReferences refs = properties.get(deviceId);
        if (refs == null) {
            refs = new PropertyReferences();
            properties.put(deviceId, refs);
        }
        return refs;
    }

    public Map<Integer, PropertyReferences> getProperties() {
        return properties;
    }

    public int size() {
        int size = 0;
        for (final PropertyReferences refs : properties.values())
            size += refs.size();
        return size;
    }

    public void clear() {
        properties.clear();
    }

    @Override
    public String toString() {
        return "DeviceObjectPropertyReferences [properties=" + properties + "]";
    }
}
