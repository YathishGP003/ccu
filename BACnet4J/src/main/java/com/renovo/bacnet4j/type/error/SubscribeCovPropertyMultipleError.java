
package com.renovo.bacnet4j.type.error;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.ChoiceOptions;
import com.renovo.bacnet4j.type.constructed.PropertyReference;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class SubscribeCovPropertyMultipleError extends BaseError {
    private static ChoiceOptions choiceOptions = new ChoiceOptions();
    static {
        choiceOptions.addContextual(0, ErrorClassAndCode.class);
        choiceOptions.addContextual(1, FirstFailedSubscription.class);
    }

    private final Choice choice;

    public SubscribeCovPropertyMultipleError(final ErrorClassAndCode errorType) {
        choice = new Choice(0, errorType, choiceOptions);
    }

    public SubscribeCovPropertyMultipleError(final FirstFailedSubscription firstFailedSubscription) {
        choice = new Choice(1, firstFailedSubscription, choiceOptions);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, choice);
    }

    public SubscribeCovPropertyMultipleError(final ByteQueue queue) throws BACnetException {
        choice = readChoice(queue, choiceOptions);
    }

    public boolean isErrorType() {
        return choice.isa(ErrorClassAndCode.class);
    }

    public ErrorClassAndCode getErrorType() {
        return choice.getDatum();
    }

    public boolean isFirstFailedSubscription() {
        return choice.isa(FirstFailedSubscription.class);
    }

    public FirstFailedSubscription getFirstFailedSubscription() {
        return choice.getDatum();
    }

    @Override
    public ErrorClassAndCode getErrorClassAndCode() {
        if (isErrorType())
            return getErrorType();
        return getFirstFailedSubscription().getErrorType();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (choice == null ? 0 : choice.hashCode());
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
        final SubscribeCovPropertyMultipleError other = (SubscribeCovPropertyMultipleError) obj;
        if (choice == null) {
            if (other.choice != null)
                return false;
        } else if (!choice.equals(other.choice))
            return false;
        return true;
    }

    public static class FirstFailedSubscription extends BaseType {
        private final ObjectIdentifier monitoredObjectIdentifier;
        private final PropertyReference monitoredPropertyReference;
        private final ErrorClassAndCode errorType;

        public FirstFailedSubscription(final ObjectIdentifier monitoredObjectIdentifier,
                final PropertyReference monitoredPropertyReference, final ErrorClassAndCode errorType) {
            this.monitoredObjectIdentifier = monitoredObjectIdentifier;
            this.monitoredPropertyReference = monitoredPropertyReference;
            this.errorType = errorType;
        }

        @Override
        public void write(final ByteQueue queue) {
            write(queue, monitoredObjectIdentifier, 0);
            write(queue, monitoredPropertyReference, 1);
            write(queue, errorType, 2);
        }

        public FirstFailedSubscription(final ByteQueue queue) throws BACnetException {
            monitoredObjectIdentifier = read(queue, ObjectIdentifier.class, 0);
            monitoredPropertyReference = read(queue, PropertyReference.class, 1);
            errorType = read(queue, ErrorClassAndCode.class, 2);
        }

        public ObjectIdentifier getMonitoredObjectIdentifier() {
            return monitoredObjectIdentifier;
        }

        public PropertyReference getMonitoredPropertyReference() {
            return monitoredPropertyReference;
        }

        public ErrorClassAndCode getErrorType() {
            return errorType;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (errorType == null ? 0 : errorType.hashCode());
            result = prime * result + (monitoredObjectIdentifier == null ? 0 : monitoredObjectIdentifier.hashCode());
            result = prime * result + (monitoredPropertyReference == null ? 0 : monitoredPropertyReference.hashCode());
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
            final FirstFailedSubscription other = (FirstFailedSubscription) obj;
            if (errorType == null) {
                if (other.errorType != null)
                    return false;
            } else if (!errorType.equals(other.errorType))
                return false;
            if (monitoredObjectIdentifier == null) {
                if (other.monitoredObjectIdentifier != null)
                    return false;
            } else if (!monitoredObjectIdentifier.equals(other.monitoredObjectIdentifier))
                return false;
            if (monitoredPropertyReference == null) {
                if (other.monitoredPropertyReference != null)
                    return false;
            } else if (!monitoredPropertyReference.equals(other.monitoredPropertyReference))
                return false;
            return true;
        }
    }
}
