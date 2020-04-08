
package com.renovo.bacnet4j.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.renovo.bacnet4j.type.constructed.PropertyReference;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class PropertyReferences implements Serializable {
    private static final long serialVersionUID = -1512876955215003611L;

    private final Map<ObjectIdentifier, List<PropertyReference>> properties = new LinkedHashMap<>();

    public PropertyReferences() {
        // no op
    }

    public PropertyReferences(final List<PropertyReferences> partitions) {
        for (final PropertyReferences that : partitions) {
            for (final Map.Entry<ObjectIdentifier, List<PropertyReference>> e : that.getProperties().entrySet()) {
                for (final PropertyReference ref : e.getValue()) {
                    add(e.getKey(), ref);
                }
            }
        }
    }

    public PropertyReferences add(final ObjectType objectType, final int objectNumber,
            final PropertyReference... refs) {
        return add(new ObjectIdentifier(objectType, objectNumber), refs);
    }

    public PropertyReferences add(final ObjectIdentifier oid, final PropertyReference... refs) {
        final List<PropertyReference> list = getOidList(oid);
        for (final PropertyReference ref : refs)
            list.add(ref);
        return this;
    }

    public PropertyReferences add(final ObjectType objectType, final int objectNumber,
            final PropertyIdentifier... pids) {
        return add(new ObjectIdentifier(objectType, objectNumber), pids);
    }

    public PropertyReferences add(final ObjectIdentifier oid, final PropertyIdentifier... pids) {
        final List<PropertyReference> list = getOidList(oid);
        for (final PropertyIdentifier pid : pids)
            list.add(new PropertyReference(pid));
        return this;
    }

    public PropertyReferences addIndex(final ObjectIdentifier oid, final PropertyIdentifier pid,
            final UnsignedInteger propertyArrayIndex) {
        final List<PropertyReference> list = getOidList(oid);
        list.add(new PropertyReference(pid, propertyArrayIndex));
        return this;
    }

    public PropertyReferences add(final PropertyReferences that) {
        for (final Map.Entry<ObjectIdentifier, List<PropertyReference>> e : that.properties.entrySet()) {
            final List<PropertyReference> existing = properties.get(e.getKey());
            if (existing == null)
                properties.put(e.getKey(), e.getValue());
            else {
                for (final PropertyReference ref : e.getValue()) {
                    if (!existing.contains(ref))
                        existing.add(ref);
                }
            }
        }
        return this;
    }

    private List<PropertyReference> getOidList(final ObjectIdentifier oid) {
        List<PropertyReference> list = properties.get(oid);
        if (list == null) {
            list = new ArrayList<>();
            properties.put(oid, list);
        }
        return list;
    }

    public Map<ObjectIdentifier, List<PropertyReference>> getProperties() {
        return properties;
    }

    public List<PropertyReferences> getPropertiesPartitioned(final int maxPartitionSize) {
        final List<PropertyReferences> partitions = new ArrayList<>();

        if (size() <= maxPartitionSize)
            partitions.add(this);
        else {
            PropertyReferences partition = null;
            List<PropertyReference> refs;
            for (final ObjectIdentifier oid : properties.keySet()) {
                refs = properties.get(oid);
                for (final PropertyReference ref : refs) {
                    if (partition == null || partition.size() >= maxPartitionSize) {
                        partition = new PropertyReferences();
                        partitions.add(partition);
                    }
                    partition.add(oid, ref);
                }
            }
        }

        return partitions;
    }

    public int size() {
        int size = 0;
        for (final List<PropertyReference> list : properties.values())
            size += list.size();
        return size;
    }

    @Override
    public String toString() {
        return "PropertyReferences [properties=" + properties + "]";
    }
}
