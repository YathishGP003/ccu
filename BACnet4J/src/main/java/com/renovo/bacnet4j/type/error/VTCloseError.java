
package com.renovo.bacnet4j.type.error;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class VTCloseError extends BaseError {
    private final ErrorClassAndCode errorType;
    private final SequenceOf<UnsignedInteger> listOfVTSessionIdentifiers;

    public VTCloseError(final ErrorClassAndCode errorType,
            final SequenceOf<UnsignedInteger> listOfVTSessionIdentifiers) {
        this.errorType = errorType;
        this.listOfVTSessionIdentifiers = listOfVTSessionIdentifiers;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, errorType, 0);
        writeOptional(queue, listOfVTSessionIdentifiers, 1);
    }

    public VTCloseError(final ByteQueue queue) throws BACnetException {
        errorType = read(queue, ErrorClassAndCode.class, 0);
        listOfVTSessionIdentifiers = readOptionalSequenceOf(queue, UnsignedInteger.class, 1);
    }

    public ErrorClassAndCode getErrorType() {
        return errorType;
    }

    public SequenceOf<UnsignedInteger> getListOfVTSessionIdentifiers() {
        return listOfVTSessionIdentifiers;
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
        result = prime * result + (listOfVTSessionIdentifiers == null ? 0 : listOfVTSessionIdentifiers.hashCode());
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
        final VTCloseError other = (VTCloseError) obj;
        if (errorType == null) {
            if (other.errorType != null)
                return false;
        } else if (!errorType.equals(other.errorType))
            return false;
        if (listOfVTSessionIdentifiers == null) {
            if (other.listOfVTSessionIdentifiers != null)
                return false;
        } else if (!listOfVTSessionIdentifiers.equals(other.listOfVTSessionIdentifiers))
            return false;
        return true;
    }
}
