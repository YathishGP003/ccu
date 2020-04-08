
package com.renovo.bacnet4j.obj;

import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.PriorityArray;
import com.renovo.bacnet4j.type.constructed.PriorityValue;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;

public class PropertyTypeDefinition {
    private final PropertyIdentifier propertyIdentifier;
    private final Class<? extends Encodable> clazz;
    private final boolean isList;
    private final boolean isArray;
    private final int arrayLength;

    PropertyTypeDefinition(final PropertyIdentifier propertyIdentifier, final Class<? extends Encodable> clazz) {
        this(propertyIdentifier, clazz, false, false, 0);
    }

    PropertyTypeDefinition(final PropertyIdentifier propertyIdentifier, final Class<? extends Encodable> clazz,
            final boolean isList) {
        this(propertyIdentifier, clazz, isList, false, 0);
    }

    PropertyTypeDefinition(final PropertyIdentifier propertyIdentifier, final Class<? extends Encodable> clazz,
            final int arrayLength) {
        this(propertyIdentifier, clazz, false, true, arrayLength);
    }

    private PropertyTypeDefinition(final PropertyIdentifier propertyIdentifier, final Class<? extends Encodable> clazz,
            final boolean isList, final boolean isArray, final int arrayLength) {
        this.propertyIdentifier = propertyIdentifier;
        this.clazz = clazz;
        this.isList = isList;
        this.isArray = isArray;
        this.arrayLength = arrayLength;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public Class<? extends Encodable> getClazz() {
        return clazz;
    }

    public boolean isList() {
        return isList;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isCollection() {
        return isList || isArray;
    }

    public int getArrayLength() {
        return arrayLength;
    }

    public Class<? extends Encodable> getInnerType() {
        if (clazz == PriorityArray.class)
            return PriorityValue.class;
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + arrayLength;
        result = prime * result + (clazz == null ? 0 : clazz.hashCode());
        result = prime * result + (isArray ? 1231 : 1237);
        result = prime * result + (isList ? 1231 : 1237);
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
        final PropertyTypeDefinition other = (PropertyTypeDefinition) obj;
        if (arrayLength != other.arrayLength)
            return false;
        if (clazz == null) {
            if (other.clazz != null)
                return false;
        } else if (!clazz.equals(other.clazz))
            return false;
        if (isArray != other.isArray)
            return false;
        if (isList != other.isList)
            return false;
        if (propertyIdentifier == null) {
            if (other.propertyIdentifier != null)
                return false;
        } else if (!propertyIdentifier.equals(other.propertyIdentifier))
            return false;
        return true;
    }
}
