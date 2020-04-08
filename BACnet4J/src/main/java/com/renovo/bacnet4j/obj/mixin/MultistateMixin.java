
package com.renovo.bacnet4j.obj.mixin;

import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AbstractMixin;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateMixin extends AbstractMixin {
    public MultistateMixin(final BACnetObject bo) {
        super(bo);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.presentValue.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger pv = (UnsignedInteger) value.getValue();
            final UnsignedInteger numStates = get(PropertyIdentifier.numberOfStates);
            if (pv.intValue() < 1 || pv.intValue() > numStates.intValue())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
                //Todo Kishore suggesting for Value out of Range Error code but as per standard it should be inconsistentConfiguration
        } else if (PropertyIdentifier.numberOfStates.equals(value.getPropertyIdentifier())) {
            final UnsignedInteger numStates = (UnsignedInteger) value.getValue();
            if (numStates.intValue() < 1)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.inconsistentConfiguration);
        } else if (PropertyIdentifier.stateText.equals(value.getPropertyIdentifier())) {
            @SuppressWarnings("unchecked")
            final BACnetArray<CharacterString> stateText = (BACnetArray<CharacterString>) value.getValue();
            final UnsignedInteger numStates = get(PropertyIdentifier.numberOfStates);
            if (numStates.intValue() != stateText.getCount())
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.inconsistentConfiguration);
        }
        return false;
    }

    @Override
    protected void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        if (PropertyIdentifier.numberOfStates.equals(pid)) {
            if (oldValue != null && !oldValue.equals(newValue)) {
                final BACnetArray<CharacterString> stateText = get(PropertyIdentifier.stateText);
                if (stateText != null) {
                    final int numStates = ((UnsignedInteger) newValue).intValue();
                    final BACnetArray<CharacterString> newText = new BACnetArray<>(numStates, CharacterString.EMPTY);

                    // Copy the old state values in.
                    final int min = newText.getCount() < stateText.getCount() ? newText.getCount()
                            : stateText.getCount();
                    for (int i = 0; i < min; i++)
                        newText.set(i, stateText.get(i));

                    writePropertyInternal(PropertyIdentifier.stateText, newText);
                }
            }
        }
    }
}
