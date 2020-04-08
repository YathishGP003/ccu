
package com.renovo.bacnet4j.type;

import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class ObjectTypePropertyReference {
    private final ObjectType objectType;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;

    public ObjectTypePropertyReference(final ObjectType objectType, final PropertyIdentifier propertyIdentifier) {
        this(objectType, propertyIdentifier, null);
    }

    public ObjectTypePropertyReference(final ObjectType objectType, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex) {
        this.objectType = objectType;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
    }

    public ObjectType getObjectType() {
        return objectType;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public UnsignedInteger getPropertyArrayIndex() {
        return propertyArrayIndex;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (objectType == null ? 0 : objectType.hashCode());
        result = prime * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = prime * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
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
        final ObjectTypePropertyReference other = (ObjectTypePropertyReference) obj;
        if (objectType == null) {
            if (other.objectType != null)
                return false;
        } else if (!objectType.equals(other.objectType))
            return false;
        if (propertyArrayIndex == null) {
            if (other.propertyArrayIndex != null)
                return false;
        } else if (!propertyArrayIndex.equals(other.propertyArrayIndex))
            return false;
        if (propertyIdentifier == null) {
            if (other.propertyIdentifier != null)
                return false;
        } else if (!propertyIdentifier.equals(other.propertyIdentifier))
            return false;
        return true;
    }
}
