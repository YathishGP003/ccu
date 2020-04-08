
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.LifeSafety;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class LifeSafetyOperationRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 27;

    private final UnsignedInteger requestingProcessIdentifier;
    private final CharacterString requestingSource;
    private final LifeSafetyOperation request;
    private final ObjectIdentifier objectIdentifier;

    public LifeSafetyOperationRequest(final UnsignedInteger requestingProcessIdentifier,
            final CharacterString requestingSource, final LifeSafetyOperation request,
            final ObjectIdentifier objectIdentifier) {
        this.requestingProcessIdentifier = requestingProcessIdentifier;
        this.requestingSource = requestingSource;
        this.request = request;
        this.objectIdentifier = objectIdentifier;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        try {
            if (objectIdentifier != null) {
                final BACnetObject bo = localDevice.getObjectRequired(objectIdentifier);
                handleForObject(from, bo, true);
            } else {
                for (final BACnetObject bo : localDevice.getLocalObjects()) {
                    handleForObject(from, bo, false);
                }
            }
        } catch (final BACnetServiceException e) {
            throw new BACnetErrorException(getChoiceId(), e.getErrorClass(), e.getErrorCode());
        }
        return null;
    }

    private void handleForObject(final Address from, final BACnetObject bo, final boolean throwOnBadType)
            throws BACnetServiceException {
        if (bo instanceof LifeSafety) {
            ((LifeSafety) bo).handleLifeSafetyOperation(from, requestingProcessIdentifier, requestingSource, request);
        } else if (throwOnBadType) {
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.unsupportedObjectType);
        }
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, requestingProcessIdentifier, 0);
        write(queue, requestingSource, 1);
        write(queue, request, 2);
        writeOptional(queue, objectIdentifier, 3);
    }

    LifeSafetyOperationRequest(final ByteQueue queue) throws BACnetException {
        requestingProcessIdentifier = read(queue, UnsignedInteger.class, 0);
        requestingSource = read(queue, CharacterString.class, 1);
        request = read(queue, LifeSafetyOperation.class, 2);
        objectIdentifier = readOptional(queue, ObjectIdentifier.class, 3);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (request == null ? 0 : request.hashCode());
        result = PRIME * result + (requestingProcessIdentifier == null ? 0 : requestingProcessIdentifier.hashCode());
        result = PRIME * result + (requestingSource == null ? 0 : requestingSource.hashCode());
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
        final LifeSafetyOperationRequest other = (LifeSafetyOperationRequest) obj;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        if (request == null) {
            if (other.request != null)
                return false;
        } else if (!request.equals(other.request))
            return false;
        if (requestingProcessIdentifier == null) {
            if (other.requestingProcessIdentifier != null)
                return false;
        } else if (!requestingProcessIdentifier.equals(other.requestingProcessIdentifier))
            return false;
        if (requestingSource == null) {
            if (other.requestingSource != null)
                return false;
        } else if (!requestingSource.equals(other.requestingSource))
            return false;
        return true;
    }
}
