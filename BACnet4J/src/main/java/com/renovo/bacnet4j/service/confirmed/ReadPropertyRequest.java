
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetRejectException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.service.acknowledgement.ReadPropertyAck;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.RejectReason;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;

public class ReadPropertyRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 12;

    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private UnsignedInteger propertyArrayIndex;

    public ReadPropertyRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
    }

    public ReadPropertyRequest(final ObjectIdentifier objectIdentifier, final PropertyIdentifier propertyIdentifier,
            final UnsignedInteger propertyArrayIndex) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, objectIdentifier, 0);
        write(queue, propertyIdentifier, 1);
        writeOptional(queue, propertyArrayIndex, 2);
    }

    ReadPropertyRequest(final ByteQueue queue) throws BACnetException {
        try {
            objectIdentifier = read(queue, ObjectIdentifier.class, 0);
            propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
            propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
        } catch (BACnetErrorException ex) {
            // 135-2016 18.9 - Confirmed request PDUs can be rejected. 
            // In the encodable class different types of errors are thrown. 
            // To meet the standard, they are converted into a reject exception.
            // We always use the rejection type "missingRequiredParameter", which covers the 135.1-2013 test standard 13.4.3 and 13.4.4.
            throw new BACnetRejectException(RejectReason.missingRequiredParameter, ex);
        }
    }

    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        Encodable prop;
        ObjectIdentifier oid = objectIdentifier;
        try {           
            //Handling for unitialized device request. See 15.5.2 and standard test 135.1-2013 9.18.1.3
            if (oid.getObjectType().equals(ObjectType.device) && oid.getInstanceNumber() == ObjectIdentifier.UNINITIALIZED) {
                oid = new ObjectIdentifier(ObjectType.device, localDevice.getInstanceNumber());
            }           
            
            // Handling for special properties
            if (propertyIdentifier.isOneOf(PropertyIdentifier.all, PropertyIdentifier.required,
                    PropertyIdentifier.optional)) {
                throw new BACnetServiceException(ErrorClass.services, ErrorCode.inconsistentParameters);
            }

            final BACnetObject obj = localDevice.getObjectRequired(oid);
            prop = obj.readPropertyRequired(propertyIdentifier, propertyArrayIndex);
        } catch (final BACnetServiceException e) {
            throw new BACnetErrorException(getChoiceId(), e);
        }
        return new ReadPropertyAck(oid, propertyIdentifier, propertyArrayIndex, prop);
    }

    public ObjectIdentifier getObjectIdentifier() {
        return objectIdentifier;
    }

    public PropertyIdentifier getPropertyIdentifier() {
        return propertyIdentifier;
    }

    public UnsignedInteger getPropertyArrayIndex() {
        return propertyArrayIndex;
    }
    
    @Override
    public String toString() {
        return "ReadPropertyRequest [objectIdentifier=" + objectIdentifier + ", propertyIdentifier="
                + propertyIdentifier + ", propertyArrayIndex=" + propertyArrayIndex + "]";
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (propertyArrayIndex == null ? 0 : propertyArrayIndex.hashCode());
        result = PRIME * result + (propertyIdentifier == null ? 0 : propertyIdentifier.hashCode());
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
        final ReadPropertyRequest other = (ReadPropertyRequest) obj;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
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
        return true;
    }
}
