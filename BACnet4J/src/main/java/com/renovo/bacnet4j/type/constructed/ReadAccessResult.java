
package com.renovo.bacnet4j.type.constructed;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.AmbiguousValue;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.ObjectTypePropertyReference;
import com.renovo.bacnet4j.type.ThreadLocalObjectTypePropertyReferenceStack;
import com.renovo.bacnet4j.type.ThreadLocalObjectTypeStack;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ReadAccessResult extends BaseType {
    private final ObjectIdentifier objectIdentifier;
    private final SequenceOf<Result> listOfResults;

    public ReadAccessResult(final ObjectIdentifier objectIdentifier, final SequenceOf<Result> listOfResults) {
        this.objectIdentifier = objectIdentifier;
        this.listOfResults = listOfResults;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        writeOptional(queue, listOfResults, 1);
    }

    @Override
    public String toString() {
        return "ReadAccessResult(oid=" + objectIdentifier + ", results=" + listOfResults + ")";
    }

    public SequenceOf<Result> getListOfResults() {
        return listOfResults;
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public ReadAccessResult(final ByteQueue queue) throws BACnetException {
        objectIdentifier = read(queue, ObjectIdentifier.class, 0);
        try {
            ThreadLocalObjectTypeStack.set(objectIdentifier.getObjectType());
            listOfResults = readOptionalSequenceOf(queue, Result.class, 1);
        } finally {
            ThreadLocalObjectTypeStack.remove();
        }
    }

    public static class Result extends BaseType {
        private static ChoiceOptions choiceOptions = new ChoiceOptions();
        static {
            choiceOptions.addContextual(4, AmbiguousValue.class);
            choiceOptions.addContextual(5, ErrorClassAndCode.class);
        }

        private final PropertyIdentifier propertyIdentifier;
        private final UnsignedInteger propertyArrayIndex;
        private final Choice readResult;

        public Result(final PropertyIdentifier propertyIdentifier, final UnsignedInteger propertyArrayIndex,
                final Encodable readResult) {
            this.propertyIdentifier = propertyIdentifier;
            this.propertyArrayIndex = propertyArrayIndex;
            this.readResult = new Choice(4, readResult, choiceOptions);
        }

        public Result(final PropertyIdentifier propertyIdentifier, final UnsignedInteger propertyArrayIndex,
                final ErrorClassAndCode readResult) {
            this.propertyIdentifier = propertyIdentifier;
            this.propertyArrayIndex = propertyArrayIndex;
            this.readResult = new Choice(5, readResult, choiceOptions);
        }

        public UnsignedInteger getPropertyArrayIndex() {
            return propertyArrayIndex;
        }

        public PropertyIdentifier getPropertyIdentifier() {
            return propertyIdentifier;
        }

        public boolean isError() {
            return readResult.isa(ErrorClassAndCode.class);
        }

        public Choice getReadResult() {
            return readResult;
        }

        @Override
        public String toString() {
            return "Result(pid=" + propertyIdentifier
                    + (propertyArrayIndex == null ? "" : ", pin=" + propertyArrayIndex) + ", value=" + readResult + ")";
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, propertyIdentifier, 2);
            writeOptional(queue, propertyArrayIndex, 3);
            write(queue, readResult);
        }

        public Result(final ByteQueue queue) throws BACnetException {
            propertyIdentifier = read(queue, PropertyIdentifier.class, 2);
            propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 3);
            try {
                ThreadLocalObjectTypePropertyReferenceStack.set(new ObjectTypePropertyReference(
                        ThreadLocalObjectTypeStack.get(), propertyIdentifier, propertyArrayIndex));
                readResult = readChoice(queue, choiceOptions);
            } finally {
                ThreadLocalObjectTypePropertyReferenceStack.remove();
            }
        }

        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
            result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
            result = PRIME * result + (readResult == null ? 0 : readResult.hashCode());
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
            final Result other = (Result) obj;
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
            if (readResult == null) {
                if (other.readResult != null)
                    return false;
            } else if (!readResult.equals(other.readResult))
                return false;
            return true;
        }
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (listOfResults == null ? 0 : listOfResults.hashCode());
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
        final ReadAccessResult other = (ReadAccessResult) obj;
        if (listOfResults == null) {
            if (other.listOfResults != null)
                return false;
        } else if (!listOfResults.equals(other.listOfResults))
            return false;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        return true;
    }
}
