package com.renovo.bacnet4j.obj;

import java.util.Objects;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.mixin.CommandableMixin;
import com.renovo.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.renovo.bacnet4j.obj.mixin.ReadOnlyPropertyMixin;
import com.renovo.bacnet4j.obj.mixin.WritablePropertyOutOfServiceMixin;
import com.renovo.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.OutOfRangeAlgo;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.LimitEnable;
import com.renovo.bacnet4j.type.constructed.OptionalReal;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.EngineeringUnits;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class AnalogOutputObject extends BACnetObject {
    public AnalogOutputObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final float presentValue, final EngineeringUnits units, final boolean outOfService,
            final float relinquishDefault) throws BACnetServiceException {
        super(localDevice, ObjectType.analogOutput, instanceNumber, name);

        Objects.requireNonNull(units);

        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.presentValue, new Real(presentValue));
        writePropertyInternal(PropertyIdentifier.units, units);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, outOfService));
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.interfaceValue, new OptionalReal());

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new CommandableMixin(this, PropertyIdentifier.presentValue));
        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.reliability));
        addMixin(new ReadOnlyPropertyMixin(this, PropertyIdentifier.ackedTransitions,
                PropertyIdentifier.eventTimeStamps, PropertyIdentifier.eventMessageTexts, PropertyIdentifier.resolution,
                PropertyIdentifier.interfaceValue));

        _supportCommandable(new Real(relinquishDefault));
        _supportValueSource();

        localDevice.addObject(this);
    }

    public AnalogOutputObject supportCovReporting(final float covIncrement) {
        _supportCovReporting(new Real(covIncrement), null);
        return this;
    }

    public AnalogOutputObject supportIntrinsicReporting(final int timeDelay, final int notificationClass,
            final float highLimit, final float lowLimit, final float deadband, final LimitEnable limitEnable,
            final EventTransitionBits eventEnable, final NotifyType notifyType, final int timeDelayNormal) {
        Objects.requireNonNull(limitEnable);
        Objects.requireNonNull(eventEnable);
        Objects.requireNonNull(notifyType);

        // Prepare the object with all of the properties that intrinsic reporting will need.
        writePropertyInternal(PropertyIdentifier.timeDelay, new UnsignedInteger(timeDelay));
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.highLimit, new Real(highLimit));
        writePropertyInternal(PropertyIdentifier.lowLimit, new Real(lowLimit));
        writePropertyInternal(PropertyIdentifier.deadband, new Real(deadband));
        writePropertyInternal(PropertyIdentifier.limitEnable, limitEnable);
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.timeDelayNormal, new UnsignedInteger(timeDelayNormal));
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, new OutOfRangeAlgo(), null, PropertyIdentifier.presentValue, //
                new PropertyIdentifier[] { PropertyIdentifier.presentValue, PropertyIdentifier.highLimit,
                        PropertyIdentifier.lowLimit, PropertyIdentifier.deadband, PropertyIdentifier.limitEnable }));

        return this;
    }
}
