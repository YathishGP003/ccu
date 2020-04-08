
package com.renovo.bacnet4j.obj;

import java.util.Objects;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.mixin.ActiveTimeMixin;
import com.renovo.bacnet4j.obj.mixin.CommandableMixin;
import com.renovo.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.renovo.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.renovo.bacnet4j.obj.mixin.StateChangeMixin;
import com.renovo.bacnet4j.obj.mixin.WritablePropertyOutOfServiceMixin;
import com.renovo.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.ChangeOfStateAlgo;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.BinaryPV;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class BinaryValueObject extends BACnetObject {
    public BinaryValueObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final BinaryPV presentValue, final boolean outOfService) throws BACnetServiceException {
        super(localDevice, ObjectType.binaryValue, instanceNumber, name);

        Objects.requireNonNull(presentValue);

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

        addMixin(new StateChangeMixin(this));

        localDevice.addObject(this);
    }

    public BinaryValueObject supportStateText(final String inactive, final String active) {
        writePropertyInternal(PropertyIdentifier.inactiveText, new CharacterString(inactive));
        writePropertyInternal(PropertyIdentifier.activeText, new CharacterString(active));
        return this;
    }

    public BinaryValueObject supportActiveTime() {
        addMixin(new ActiveTimeMixin(this, false));
        return this;
    }

    public BinaryValueObject supportIntrinsicReporting(final int timeDelay, final int notificationClass,
            final BinaryPV alarmValue, final EventTransitionBits eventEnable, final NotifyType notifyType,
            final int timeDelayNormal) {
        Objects.requireNonNull(alarmValue);
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        // Prepare the object with all of the properties that intrinsic reporting will need.
        // User-defined properties
        writePropertyInternal(PropertyIdentifier.timeDelay, new UnsignedInteger(timeDelay));
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.alarmValue, alarmValue);
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.timeDelayNormal, new UnsignedInteger(timeDelayNormal));
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        final ChangeOfStateAlgo eventAlgo = new ChangeOfStateAlgo(PropertyIdentifier.presentValue,
                PropertyIdentifier.alarmValue);
        addMixin(new IntrinsicReportingMixin(this, eventAlgo, null, PropertyIdentifier.presentValue,
                new PropertyIdentifier[] { PropertyIdentifier.presentValue, PropertyIdentifier.alarmValue }));
        return this;
    }

    public BinaryValueObject supportCovReporting() {
        _supportCovReporting(null, null);
        return this;
    }

    public BinaryValueObject supportCommandable(final BinaryPV relinquishDefault) {
        Objects.requireNonNull(relinquishDefault);
        super._supportCommandable(relinquishDefault);
        return this;
    }

    public BinaryValueObject supportValueSource() {
        super._supportValueSource();
        return this;
    }

    public BinaryValueObject supportWritable() {
        _supportWritable();
        return this;
    }
}
