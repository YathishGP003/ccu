/**
 * Copyright (C) 2018 Infinite Automation Systems, Inc. All rights reserved
 * 
 */
package com.renovo.bacnet4j.obj;

import java.util.Objects;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.mixin.CommandableMixin;
import com.renovo.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.renovo.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.renovo.bacnet4j.obj.mixin.WritablePropertyOutOfServiceMixin;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;

/**
 *
 * @author Phillip Dunlap, based on BinaryValueObject
 */
public class CharacterStringObject extends BACnetObject {

    /**
     * @param localDevice
     * @param instanceNumber
     * @param name
     * @param presentValue
     * @param outOfService
     * @throws BACnetServiceException 
     */
    public CharacterStringObject(LocalDevice localDevice, int instanceNumber,
            String name, final CharacterString presentValue, final boolean outOfService) throws BACnetServiceException {
        super(localDevice, ObjectType.characterstringValue, instanceNumber, name);
        

        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, outOfService));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new CommandableMixin(this, PropertyIdentifier.presentValue));
        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.reliability));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.ackedTransitions,
                PropertyIdentifier.eventTimeStamps, PropertyIdentifier.eventMessageTexts));
        
        writePropertyInternal(PropertyIdentifier.presentValue, presentValue);
        
        localDevice.addObject(this);
    }
    
    public CharacterStringObject supportCommandable(final CharacterString relinquishDefault) {
        Objects.requireNonNull(relinquishDefault);
        super._supportCommandable(relinquishDefault);
        return this;
    }
    
    public CharacterStringObject supportCovReporting() {
        _supportCovReporting(null, null);
        return this;
    }
    
    

}
