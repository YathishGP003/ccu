
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.ThreadLocalObjectTypeStack;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class WriteAccessSpecification extends BaseType {
    private final ObjectIdentifier objectIdentifier;
    private final SequenceOf<PropertyValue> listOfProperties;

    public WriteAccessSpecification(final ObjectIdentifier objectIdentifier,
            final SequenceOf<PropertyValue> listOfProperties) {
        this.objectIdentifier = objectIdentifier;
        this.listOfProperties = listOfProperties;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, listOfProperties, 1);
    }

    public WriteAccessSpecification(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        try {
            ThreadLocalObjectTypeStack.set(objectIdentifier.getObjectType());
            listOfProperties = readSequenceOf(queue, PropertyValue.class, 1);
        } finally {
            ThreadLocalObjectTypeStack.remove();
        }
    }

    public SequenceOf<PropertyValue> getListOfProperties() {
        return listOfProperties;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public int size() {
        return listOfProperties.getCount();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfProperties == null ? 0 : listOfProperties.hashCode());
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
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
        final WriteAccessSpecification other = (WriteAccessSpecification) obj;
        if (listOfProperties == null) {
            if (other.listOfProperties != null)
                return false;
        } else if (!listOfProperties.equals(other.listOfProperties))
            return false;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "WriteAccessSpecification [objectIdentifier=" + objectIdentifier + ", listOfProperties=" + listOfProperties + ']';
    }
}
