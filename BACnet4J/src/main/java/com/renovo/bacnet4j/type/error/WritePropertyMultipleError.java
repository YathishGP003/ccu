
package com.renovo.bacnet4j.type.error;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class WritePropertyMultipleError extends BaseError {
    private final ErrorClassAndCode errorType;
    private final ObjectPropertyReference firstFailedWriteAttempt;

    public WritePropertyMultipleError(final ErrorClassAndCode errorType,
            final ObjectPropertyReference firstFailedWriteAttempt) {
        this.errorType = errorType;
        this.firstFailedWriteAttempt = firstFailedWriteAttempt;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, errorType, 0);
        firstFailedWriteAttempt.write(queue, 1);
    }

    public WritePropertyMultipleError(final ByteQueue queue) throws BACnetException {
        errorType = read(queue, ErrorClassAndCode.class, 0);
        firstFailedWriteAttempt = read(queue, ObjectPropertyReference.class, 1);
    }

    public ErrorClassAndCode getErrorType() {
        return errorType;
    }

    public ObjectPropertyReference getFirstFailedWriteAttempt() {
        return firstFailedWriteAttempt;
    }

    @Override
    public ErrorClassAndCode getErrorClassAndCode() {
        return errorType;
    }

    @Override
    public String toString() {
        return "WritePropertyMultipleError [firstFailedWriteAttempt=" + firstFailedWriteAttempt + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (errorType == null ? 0 : errorType.hashCode());
        result = prime * result + (firstFailedWriteAttempt == null ? 0 : firstFailedWriteAttempt.hashCode());
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
        final WritePropertyMultipleError other = (WritePropertyMultipleError) obj;
        if (errorType == null) {
            if (other.errorType != null)
                return false;
        } else if (!errorType.equals(other.errorType))
            return false;
        if (firstFailedWriteAttempt == null) {
            if (other.firstFailedWriteAttempt != null)
                return false;
        } else if (!firstFailedWriteAttempt.equals(other.firstFailedWriteAttempt))
            return false;
        return true;
    }
}
