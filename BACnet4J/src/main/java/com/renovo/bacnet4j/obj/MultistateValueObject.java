
package com.renovo.bacnet4j.obj;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.mixin.CommandableMixin;
import com.renovo.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.renovo.bacnet4j.obj.mixin.MultistateMixin;
import com.renovo.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.renovo.bacnet4j.obj.mixin.WritablePropertyOutOfServiceMixin;
import com.renovo.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.ChangeOfStateAlgo;
import com.renovo.bacnet4j.obj.mixin.event.faultAlgo.FaultStateAlgo;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.DeviceObjectReference;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class MultistateValueObject extends BACnetObject {
    public MultistateValueObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final int numberOfStates, final BACnetArray<CharacterString> stateText, final int presentValueBase1,
            final boolean outOfService) throws BACnetServiceException {
        super(localDevice, ObjectType.multiStateValue, instanceNumber, name);

        if (numberOfStates < 1) {
            throw new IllegalArgumentException("numberOfStates cannot be less than 1");
        }

        final ValueSource valueSource = new ValueSource(new DeviceObjectReference(localDevice.getId(), getId()));

        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writeProperty(valueSource, PropertyIdentifier.presentValue, new UnsignedInteger(presentValueBase1));
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.TRUE);
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, true));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new CommandableMixin(this, PropertyIdentifier.presentValue));
        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.reliability));
        addMixin(new MultistateMixin(this));

        writePropertyInternal(PropertyIdentifier.numberOfStates, new UnsignedInteger(numberOfStates));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.numberOfStates));
        if (stateText != null) {
            if (numberOfStates != stateText.getCount()) {
                throw new IllegalArgumentException("numberOfStates does not match state text count");
            }
            writeProperty(null, PropertyIdentifier.stateText, stateText);
        }
        writeProperty(valueSource, PropertyIdentifier.presentValue, new UnsignedInteger(presentValueBase1));
        if (!outOfService) {
            writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        }

        localDevice.addObject(this);
    }

    public MultistateValueObject supportIntrinsicReporting(final int timeDelay, final int notificationClass,
            final BACnetArray<UnsignedInteger> alarmValues, final BACnetArray<UnsignedInteger> faultValues,
            final EventTransitionBits eventEnable, final NotifyType notifyType, final int timeDelayNormal) {
        // Prepare the object with all of the properties that intrinsic reporting will need.
        // User-defined properties
        writePropertyInternal(PropertyIdentifier.timeDelay, new UnsignedInteger(timeDelay));
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.alarmValues, alarmValues);
        if (faultValues != null)
            writePropertyInternal(PropertyIdentifier.faultValues, faultValues);
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.timeDelayNormal, new UnsignedInteger(timeDelayNormal));
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        // Now add the mixin.
        final ChangeOfStateAlgo eventAlgo = new ChangeOfStateAlgo(PropertyIdentifier.presentValue,
                PropertyIdentifier.alarmValues);
        FaultStateAlgo faultAlgo = null;
        if (faultValues != null) {
            faultAlgo = new FaultStateAlgo(PropertyIdentifier.reliability, PropertyIdentifier.faultValues);
        }
        addMixin(new IntrinsicReportingMixin(this, eventAlgo, faultAlgo, PropertyIdentifier.presentValue,
                new PropertyIdentifier[] { PropertyIdentifier.presentValue }));

        return this;
    }

    public MultistateValueObject supportCovReporting() {
        _supportCovReporting(null, null);
        return this;
    }

    public MultistateValueObject supportCommandable(final UnsignedInteger relinquishDefault) {
        _supportCommandable(relinquishDefault);
        return this;
    }

    public MultistateValueObject supportValueSource() {
        _supportValueSource();
        return this;
    }

    public MultistateValueObject supportWritable() {
        _supportWritable();
        return this;
    }
}
