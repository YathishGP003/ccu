
package com.renovo.bacnet4j.type.error;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class CreateObjectError extends BaseError {
    private final ErrorClassAndCode errorType;
    private final UnsignedInteger firstFailedElementNumber;

    public CreateObjectError(final ErrorClassAndCode errorType, final UnsignedInteger firstFailedElementNumber) {
        this.errorType = errorType;
        this.firstFailedElementNumber = firstFailedElementNumber;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, errorType, 0);
        write(queue, firstFailedElementNumber, 1);
    }

    public CreateObjectError(final ByteQueue queue) throws BACnetException {
        errorType = read(queue, ErrorClassAndCode.class, 0);
        firstFailedElementNumber = read(queue, UnsignedInteger.class, 1);
    }

    public ErrorClassAndCode getErrorType() {
        return errorType;
    }

    public UnsignedInteger getFirstFailedElementNumber() {
        return firstFailedElementNumber;
    }

    @Override
    public ErrorClassAndCode getErrorClassAndCode() {
        return errorType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (errorType == null ? 0 : errorType.hashCode());
        result = prime * result + (firstFailedElementNumber == null ? 0 : firstFailedElementNumber.hashCode());
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
        final CreateObjectError other = (CreateObjectError) obj;
        if (errorType == null) {
            if (other.errorType != null)
                return false;
        } else if (!errorType.equals(other.errorType))
            return false;
        if (firstFailedElementNumber == null) {
            if (other.firstFailedElementNumber != null)
                return false;
        } else if (!firstFailedElementNumber.equals(other.firstFailedElementNumber))
            return false;
        return true;
    }
}
