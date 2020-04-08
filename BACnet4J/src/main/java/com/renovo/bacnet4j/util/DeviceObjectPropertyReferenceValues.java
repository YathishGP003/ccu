
package com.renovo.bacnet4j.util;

import java.util.LinkedHashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

public class DeviceObjectPropertyReferenceValues {
    private final Map<Integer, PropertyReferenceValues> properties = new LinkedHashMap<>();

    public DeviceObjectPropertyReferenceValues add(final int deviceId, final ObjectType objectType,
            final int objectNumber, final PropertyValue... values) {
        return add(deviceId, new ObjectIdentifier(objectType, objectNumber), values);
    }

    public DeviceObjectPropertyReferenceValues add(final int deviceId, final ObjectIdentifier oid,
            final PropertyValue... values) {
        final PropertyReferenceValues refs = getDeviceProperties(deviceId);
        for (final PropertyValue value : values)
            refs.add(oid, value);
        return this;
    }

    public DeviceObjectPropertyReferenceValues add(final int deviceId, final PropertyReferenceValues values) {
        final PropertyReferenceValues existing = properties.get(deviceId);
        if (existing == null)
            properties.put(deviceId, values);
        else
            existing.add(values);
        return this;
    }

    public PropertyReferenceValues getDeviceProperties(final Integer deviceId) {
        PropertyReferenceValues values = properties.get(deviceId);
        if (values == null) {
            values = new PropertyReferenceValues();
            properties.put(deviceId, values);
        }
        return values;
    }

    public Map<Integer, PropertyReferenceValues> getProperties() {
        return properties;
    }

    public int size() {
        int size = 0;
        for (final PropertyReferenceValues refs : properties.values())
            size += refs.size();
        return size;
    }

    public void clear() {
        properties.clear();
    }
}
