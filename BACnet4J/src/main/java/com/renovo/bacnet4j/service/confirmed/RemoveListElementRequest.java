
package com.renovo.bacnet4j.service.confirmed;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetErrorException;
import com.renovo.bacnet4j.exception.BACnetException;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.ObjectProperties;
import com.renovo.bacnet4j.obj.ObjectPropertyTypeDefinition;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.renovo.bacnet4j.type.Encodable;
import static com.renovo.bacnet4j.type.Encodable.read;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.error.ChangeListError;
import com.renovo.bacnet4j.type.error.ErrorClassAndCode;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.ArrayList;
import java.util.List;

public class RemoveListElementRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 9;

    private final ObjectIdentifier objectIdentifier;
    private final PropertyIdentifier propertyIdentifier;
    private final UnsignedInteger propertyArrayIndex;
    private final SequenceOf<? extends Encodable> listOfElements;

    public RemoveListElementRequest(final ObjectIdentifier objectIdentifier,
            final PropertyIdentifier propertyIdentifier, final UnsignedInteger propertyArrayIndex,
            final SequenceOf<? extends Encodable> listOfElements) {
        this.objectIdentifier = objectIdentifier;
        this.propertyIdentifier = propertyIdentifier;
        this.propertyArrayIndex = propertyArrayIndex;
        this.listOfElements = listOfElements;
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
        write(queue, listOfElements, 3);
    }

    RemoveListElementRequest(final ByteQueue queue) throws BACnetException {
        int errorIndex = 0;
        try {
            objectIdentifier = read(queue, ObjectIdentifier.class, 0);
            propertyIdentifier = read(queue, PropertyIdentifier.class, 1);
            propertyArrayIndex = readOptional(queue, UnsignedInteger.class, 2);
            // We have to encode the values here because we need the correct 
            // errorIndex to fulfill the standard test 135.1-2013 9.14.2.3
            final ObjectPropertyTypeDefinition def = ObjectProperties
                    .getObjectPropertyTypeDefinition(objectIdentifier.getObjectType(), propertyIdentifier);
            popStart(queue, 3);
            List<Encodable> values = new ArrayList<>();
            while (readEnd(queue) != 3) {
                errorIndex++;
                values.add(read(queue, def.getPropertyTypeDefinition().getClazz()));
            }
            popEnd(queue, 3);
            listOfElements = new SequenceOf<>(values);
        } catch (BACnetErrorException ex) {
            ErrorClassAndCode errorClassAndCode = ex.getBacnetError().getError().getErrorClassAndCode();
            throw createException(errorClassAndCode.getErrorClass(), errorClassAndCode.getErrorCode(), new UnsignedInteger(errorIndex));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public AcknowledgementService handle(final LocalDevice localDevice, final Address from) throws BACnetException {
        final BACnetObject obj = localDevice.getObject(objectIdentifier);
        if (obj == null)
            throw createException(ErrorClass.object, ErrorCode.unknownObject, UnsignedInteger.ZERO);

        final PropertyValue pv = new PropertyValue(propertyIdentifier, propertyArrayIndex, listOfElements, null);
        if (!localDevice.getEventHandler().checkAllowPropertyWrite(from, obj, pv))
            throw createException(ErrorClass.property, ErrorCode.writeAccessDenied, UnsignedInteger.ZERO);

        ObjectPropertyTypeDefinition def = ObjectProperties
                .getObjectPropertyTypeDefinition(objectIdentifier.getObjectType(), propertyIdentifier);

        Encodable e;
        try {
            e = obj.readPropertyRequired(propertyIdentifier);
        } catch (final BACnetServiceException ex) {
            throw createException(ex.getErrorClass(), ex.getErrorCode(), UnsignedInteger.ZERO);
        }

        BACnetArray<Encodable> array = null;
        if (propertyArrayIndex != null) {
            // The property must be an array.
            if (!(e instanceof BACnetArray))
                throw createException(ErrorClass.property, ErrorCode.propertyIsNotAnArray, UnsignedInteger.ZERO);

            array = (BACnetArray<Encodable>) e;

            // Check the requested index.
            final int index = propertyArrayIndex.intValue();
            if (index < 1 || index > array.getCount())
                throw createException(ErrorClass.property, ErrorCode.invalidArrayIndex, UnsignedInteger.ZERO);

            e = array.getBase1(index);
            if (e == null)
                // This should never happen, but check.
                throw new RuntimeException("Array with null element: " + array);

            // Set the def null because we don't know the inner type of a list in an array.
            def = null;
        }

        // The value we end up with must be a list.
        if (!(e instanceof SequenceOf))
            throw createException(ErrorClass.services, ErrorCode.propertyIsNotAList, UnsignedInteger.ZERO);
        if (e instanceof BACnetArray)
            throw createException(ErrorClass.services, ErrorCode.propertyIsNotAList, UnsignedInteger.ZERO);

        //Copy the original sequence
        final SequenceOf<Encodable> copyList = new SequenceOf<>(new ArrayList(((SequenceOf<Encodable>) e).getValues()));   
        final SequenceOf<Encodable> list = new SequenceOf<>(copyList.getValues());
        for (int i = 0; i < listOfElements.getCount(); i++) {
            final Encodable pr = listOfElements.getBase1(i + 1);

            if (def != null) {
                // If we have a property def, check it.
                if (!def.getPropertyTypeDefinition().getClazz().isAssignableFrom(pr.getClass())) {
                    throw createException(ErrorClass.property, ErrorCode.invalidDataType, new UnsignedInteger(i + 1));
                }
            } else {
                // Otherwise, if there are already elements in the list, we can check against the first one.
                if (list.getCount() > 0) {
                    if (list.getBase1(1).getClass() != pr.getClass()) {
                        throw createException(ErrorClass.property, ErrorCode.invalidDataType,
                                new UnsignedInteger(i + 1));
                    }
                }
            }

            if (!list.contains(pr))
                throw createException(ErrorClass.services, ErrorCode.listElementNotFound, new UnsignedInteger(i + 1));
            list.remove(pr);
        }

        if (array != null) {
            array.setBase1(propertyArrayIndex.intValue(), copyList);
            e = array;
        } else {
            e = copyList;
        }

        try {
            obj.writeProperty(new ValueSource(from), propertyIdentifier, e);
        } catch (final BACnetServiceException ex) {
            throw createException(ex.getErrorClass(), ex.getErrorCode(), UnsignedInteger.ZERO);
        }

        localDevice.getEventHandler().propertyWritten(from, obj, pv);

        return null;
    }

    private BACnetErrorException createException(final ErrorClass errorClass, final ErrorCode errorCode,
            final UnsignedInteger firstFailedElementNumber) {
        return new BACnetErrorException(getChoiceId(),
                new ChangeListError(new ErrorClassAndCode(errorClass, errorCode), firstFailedElementNumber));
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (objectIdentifier == null ? 0 : objectIdentifier.hashCode());
        result = PRIME * result + (listOfElements == null ? 0 : listOfElements.hashCode());
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
        final RemoveListElementRequest other = (RemoveListElementRequest) obj;
        if (objectIdentifier == null) {
            if (other.objectIdentifier != null)
                return false;
        } else if (!objectIdentifier.equals(other.objectIdentifier))
            return false;
        if (listOfElements == null) {
            if (other.listOfElements != null)
                return false;
        } else if (!listOfElements.equals(other.listOfElements))
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
