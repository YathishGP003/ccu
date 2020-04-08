
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.event.PrivateTransferHandler;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.acknowledgement.ConfirmedPrivateTransferAck;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.EncodedValue;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.error.ConfirmedPrivateTransferError;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ConfirmedPrivateTransferRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 18;

    private final UnsignedInteger vendorId;
    private final UnsignedInteger serviceNumber;
    private final Encodable serviceParameters;

    public ConfirmedPrivateTransferRequest(final int vendorId, final int serviceNumber,
            final Encodable serviceParameters) {
        this(new UnsignedInteger(vendorId), new UnsignedInteger(serviceNumber), serviceParameters);
    }

    public ConfirmedPrivateTransferRequest(final UnsignedInteger vendorId, final UnsignedInteger serviceNumber,
            final Encodable serviceParameters) {
        this.vendorId = vendorId;
        this.serviceNumber = serviceNumber;
        this.serviceParameters = serviceParameters;
    }

    ConfirmedPrivateTransferRequest(final ByteQueue queue) throws BACnetException {
        vendorId = read(queue, UnsignedInteger.class, 0);
        serviceNumber = read(queue, UnsignedInteger.class, 1);
        serviceParameters = readEncodedValue(queue, 2);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, vendorId, 0);
        write(queue, serviceNumber, 1);
        writeOptional(queue, serviceParameters, 2);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final PrivateTransferHandler handler = localDevice.getPrivateTransferHandler(vendorId, serviceNumber);

        if (handler == null) {
            throw new BACnetErrorException(TYPE_ID,
                    new ConfirmedPrivateTransferError(
                            new ErrorClassAndCode(ErrorClass.services, ErrorCode.optionalFunctionalityNotSupported),
                            vendorId, serviceNumber, new CharacterString("No handler for vendorId/serviceNumber")));
        }

        final Encodable resultBlock = handler.handle(localDevice, from, vendorId, serviceNumber,
                (EncodedValue) serviceParameters, true);

        return new ConfirmedPrivateTransferAck(vendorId, serviceNumber, resultBlock);
    }

    public UnsignedInteger getVendorId() {
        return vendorId;
    }

    public UnsignedInteger getServiceNumber() {
        return serviceNumber;
    }

    public Encodable getServiceParameters() {
        return serviceParameters;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (serviceNumber == null ? 0 : serviceNumber.hashCode());
        result = PRIME * result + (serviceParameters == null ? 0 : serviceParameters.hashCode());
        result = PRIME * result + (vendorId == null ? 0 : vendorId.hashCode());
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
        final ConfirmedPrivateTransferRequest other = (ConfirmedPrivateTransferRequest) obj;
        if (serviceNumber == null) {
            if (other.serviceNumber != null)
                return false;
        } else if (!serviceNumber.equals(other.serviceNumber))
            return false;
        if (serviceParameters == null) {
            if (other.serviceParameters != null)
                return false;
        } else if (!serviceParameters.equals(other.serviceParameters))
            return false;
        if (vendorId == null) {
            if (other.vendorId != null)
                return false;
        } else if (!vendorId.equals(other.vendorId))
            return false;
        return true;
    }
}
