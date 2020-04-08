
package com.renovo.bacnet4j.type.constructed;

import java.util.ArrayList;
import java.util.List;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ReadAccessSpecification extends BaseType {
    private final ObjectIdentifier objectIdentifier;
    private final SequenceOf<PropertyReference> listOfPropertyReferences;

    public ReadAccessSpecification(final ObjectIdentifier objectIdentifier,
            final SequenceOf<PropertyReference> listOfPropertyReferences) {
        this.objectIdentifier = objectIdentifier;
        this.listOfPropertyReferences = listOfPropertyReferences;
    }

    public ReadAccessSpecification(final ObjectIdentifier objectIdentifier, final PropertyIdentifier pid) {
        this.objectIdentifier = objectIdentifier;
        final List<PropertyReference> refs = new ArrayList<>(1);
        refs.add(new PropertyReference(pid, null));
        this.listOfPropertyReferences = new SequenceOf<>(refs);
    }

    public SequenceOf<PropertyReference> getListOfPropertyReferences() {
        return listOfPropertyReferences;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, listOfPropertyReferences, 1);
    }

    public ReadAccessSpecification(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        listOfPropertyReferences = readSequenceOf(queue, PropertyReference.class, 1);
    }

    public int getNumberOfProperties() {
        return listOfPropertyReferences.getCount();
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfPropertyReferences == null ? 0 : listOfPropertyReferences.hashCode());
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
        final ReadAccessSpecification other = (ReadAccessSpecification) obj;
        if (listOfPropertyReferences == null) {
            if (other.listOfPropertyReferences != null)
                return false;
        } else if (!listOfPropertyReferences.equals(other.listOfPropertyReferences))
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
        return "ReadAccessSpecification [objectIdentifier=" + objectIdentifier + ", listOfPropertyReferences="
                + listOfPropertyReferences + "]";
    }
}
