
package com.renovo.bacnet4j.type.error;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.util.sero.ByteQueue;

/**
 * Represents the Error sequence
 */
public class ErrorClassAndCode extends BaseError {
    private final ErrorClass errorClass;
    private final ErrorCode errorCode;

    public ErrorClassAndCode(final ErrorClass errorClass, final ErrorCode errorCode) {
        this.errorClass = errorClass;
        this.errorCode = errorCode;
    }

    public ErrorClassAndCode(final BACnetServiceException e) {
        this.errorClass = e.getErrorClass();
        this.errorCode = e.getErrorCode();
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, errorClass);
        write(queue, errorCode);
    }

    public ErrorClassAndCode(final ByteQueue queue) throws BACnetException {
        errorClass = read(queue, ErrorClass.class);
        errorCode = read(queue, ErrorCode.class);
    }

    public ErrorClass getErrorClass() {
        return errorClass;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public ErrorClassAndCode getErrorClassAndCode() {
        return this;
    }

    public boolean equals(final ErrorClass errorClass, final ErrorCode errorCode) {
        return this.errorClass.equals(errorClass) && this.errorCode.equals(errorCode);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (errorClass == null ? 0 : errorClass.hashCode());
        result = PRIME * result + (errorCode == null ? 0 : errorCode.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "errorClass=" + errorClass + ", errorCode=" + errorCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ErrorClassAndCode other = (ErrorClassAndCode) obj;
        if (errorClass == null) {
            if (other.errorClass != null)
                return false;
        } else if (!errorClass.equals(other.errorClass))
            return false;
        if (errorCode == null) {
            if (other.errorCode != null)
                return false;
        } else if (!errorCode.equals(other.errorCode))
            return false;
        return true;
    }
}
