
package com.renovo.bacnet4j.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.renovo.bacnet4j.exception.PropertyValueException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.PropertyReference;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class PropertyValues implements Iterable<ObjectPropertyReference> {
    private final Map<ObjectPropertyReference, Encodable> values = new HashMap<>();

    public void add(final ObjectIdentifier oid, final PropertyIdentifier pid, final UnsignedInteger pin,
            final Encodable value) {
        values.put(new ObjectPropertyReference(oid, pid, pin), value);
    }

    public Encodable getNoErrorCheck(final ObjectPropertyReference opr) {
        return values.get(opr);
    }

    public Encodable get(final ObjectPropertyReference opr) throws PropertyValueException {
        final Encodable e = getNoErrorCheck(opr);

        if (e instanceof ErrorClassAndCode)
            throw new PropertyValueException((ErrorClassAndCode) e);

        return e;
    }

    public Encodable getNoErrorCheck(final ObjectIdentifier oid, final PropertyReference ref) {
        return getNoErrorCheck(
                new ObjectPropertyReference(oid, ref.getPropertyIdentifier(), ref.getPropertyArrayIndex()));
    }

    public Encodable getNoErrorCheck(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return getNoErrorCheck(new ObjectPropertyReference(oid, pid));
    }

    public Encodable get(final ObjectIdentifier oid, final PropertyIdentifier pid) throws PropertyValueException {
        return get(new ObjectPropertyReference(oid, pid));
    }

    public Encodable getNullOnError(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return getNullOnError(getNoErrorCheck(new ObjectPropertyReference(oid, pid)));
    }

    public String getString(final ObjectIdentifier oid, final PropertyIdentifier pid) {
        return getString(getNoErrorCheck(oid, pid));
    }

    public String getString(final ObjectIdentifier oid, final PropertyIdentifier pid, final String defaultValue) {
        return getString(getNoErrorCheck(oid, pid), defaultValue);
    }

    @Override
    public Iterator<ObjectPropertyReference> iterator() {
        return values.keySet().iterator();
    }

    public static String getString(final Encodable value) {
        if (value == null)
            return null;
        return value.toString();
    }

    public static String getString(final Encodable value, final String defaultValue) {
        if (value == null || value instanceof ErrorClassAndCode)
            return defaultValue;
        return value.toString();
    }

    public static Encodable getNullOnError(final Encodable value) {
        if (value instanceof ErrorClassAndCode)
            return null;
        return value;
    }

    public int size() {
        return values.size();
    }

    @Override
    public String toString() {
        return "PropertyValues [values=" + values + "]";
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
        final PropertyValues other = (PropertyValues) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }
}
