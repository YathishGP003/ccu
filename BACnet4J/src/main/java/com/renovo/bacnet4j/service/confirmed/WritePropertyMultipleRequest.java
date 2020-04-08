
package com.renovo.bacnet4j.service.confirmed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.constructed.WriteAccessSpecification;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;
import com.renovo.bacnet4j.type.error.WritePropertyMultipleError;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class WritePropertyMultipleRequest extends ConfirmedRequestService {
    static final Logger LOG = LoggerFactory.getLogger(WritePropertyMultipleRequest.class);
    public static final byte TYPE_ID = 16;

    private final SequenceOf<WriteAccessSpecification> listOfWriteAccessSpecifications;

    public WritePropertyMultipleRequest(final SequenceOf<WriteAccessSpecification> listOfWriteAccessSpecifications) {
        this.listOfWriteAccessSpecifications = listOfWriteAccessSpecifications;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, listOfWriteAccessSpecifications);
    }

    WritePropertyMultipleRequest(final ByteQueue queue) throws BACnetException {
        listOfWriteAccessSpecifications = readSequenceOf(queue, WriteAccessSpecification.class);
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        BACnetObject obj;
        for (final WriteAccessSpecification spec : listOfWriteAccessSpecifications) {
            obj = localDevice.getObject(spec.getObjectIdentifier());
            if (obj == null)
                throw createException(ErrorClass.property, ErrorCode.unknownObject, spec, null);

            for (final PropertyValue pv : spec.getListOfProperties()) {
                LOG.info("Writing property {} into {}", pv, obj);
                try {
                    if (localDevice.getEventHandler().checkAllowPropertyWrite(from, obj, pv)) {
                        obj.writeProperty(new ValueSource(from), pv);
                        localDevice.getEventHandler().propertyWritten(from, obj, pv);
                    } else
                        throw createException(ErrorClass.property, ErrorCode.writeAccessDenied, spec, pv);
                } catch (final BACnetServiceException e) {
                    throw createException(e.getErrorClass(), e.getErrorCode(), spec, pv);
                }
            }
        }

        return null;
    }

    private BACnetErrorException createException(final ErrorClass errorClass, final ErrorCode errorCode,
            final WriteAccessSpecification spec, final PropertyValue pv) {
        final PropertyValue pvToUse = pv == null ? spec.getListOfProperties().getBase1(1) : pv;
        return new BACnetErrorException(getChoiceId(),
                new WritePropertyMultipleError(new ErrorClassAndCode(errorClass, errorCode),
                        new ObjectPropertyReference(spec.getObjectIdentifier(), pvToUse.getPropertyIdentifier(),
                                pvToUse.getPropertyArrayIndex())));
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result
                + (listOfWriteAccessSpecifications == null ? 0 : listOfWriteAccessSpecifications.hashCode());
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
        final WritePropertyMultipleRequest other = (WritePropertyMultipleRequest) obj;
        if (listOfWriteAccessSpecifications == null) {
            if (other.listOfWriteAccessSpecifications != null)
                return false;
        } else if (!listOfWriteAccessSpecifications.equals(other.listOfWriteAccessSpecifications))
            return false;
        return true;
    }
}
