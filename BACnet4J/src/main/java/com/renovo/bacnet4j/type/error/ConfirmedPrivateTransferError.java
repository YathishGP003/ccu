
package com.renovo.bacnet4j.type.error;

import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ConfirmedPrivateTransferError extends BaseError {
    private final ErrorClassAndCode errorType;
    private final UnsignedInteger vendorId;
    private final UnsignedInteger serviceNumber;
    private final Encodable errorParameters;

    public ConfirmedPrivateTransferError(final ErrorClassAndCode errorType, final UnsignedInteger vendorId,
            final UnsignedInteger serviceNumber, final Encodable errorParameters) {
        this.errorType = errorType;
        this.vendorId = vendorId;
        this.serviceNumber = serviceNumber;
        this.errorParameters = errorParameters;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, errorType, 0);
        write(queue, vendorId, 1);
        write(queue, serviceNumber, 2);
        writeOptional(queue, errorParameters, 3);
    }

    public ConfirmedPrivateTransferError(final ByteQueue queue) throws BACnetException {
        errorType = read(queue, ErrorClassAndCode.class, 0);
        vendorId = read(queue, UnsignedInteger.class, 1);
        serviceNumber = read(queue, UnsignedInteger.class, 2);
        errorParameters = readEncodedValue(queue, 3);
    }

    public ErrorClassAndCode getErrorType() {
        return errorType;
    }

    public UnsignedInteger getVendorId() {
        return vendorId;
    }

    public UnsignedInteger getServiceNumber() {
        return serviceNumber;
    }

    public Encodable getErrorParameters() {
        return errorParameters;
    }

    @Override
    public ErrorClassAndCode getErrorClassAndCode() {
        return errorType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (errorParameters == null ? 0 : errorParameters.hashCode());
        result = prime * result + (errorType == null ? 0 : errorType.hashCode());
        result = prime * result + (serviceNumber == null ? 0 : serviceNumber.hashCode());
        result = prime * result + (vendorId == null ? 0 : vendorId.hashCode());
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
        final ConfirmedPrivateTransferError other = (ConfirmedPrivateTransferError) obj;
        if (errorParameters == null) {
            if (other.errorParameters != null)
                return false;
        } else if (!errorParameters.equals(other.errorParameters))
            return false;
        if (errorType == null) {
            if (other.errorType != null)
                return false;
        } else if (!errorType.equals(other.errorType))
            return false;
        if (serviceNumber == null) {
            if (other.serviceNumber != null)
                return false;
        } else if (!serviceNumber.equals(other.serviceNumber))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }
}
