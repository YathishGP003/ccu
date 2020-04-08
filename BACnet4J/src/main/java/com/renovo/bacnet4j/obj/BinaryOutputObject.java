
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
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.CommandFailureAlgo;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.OptionalBinaryPV;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.BinaryPV;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.Polarity;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class BinaryOutputObject extends BACnetObject {
    public BinaryOutputObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final BinaryPV presentValue, final boolean outOfService, final Polarity polarity,
            final BinaryPV relinquishDefault) throws BACnetServiceException {
        super(localDevice, ObjectType.binaryOutput, instanceNumber, name);

        Objects.requireNonNull(presentValue);
        Objects.requireNonNull(polarity);
        Objects.requireNonNull(relinquishDefault);

        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.outOfService, Boolean.valueOf(outOfService));
        writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, outOfService));

        // Mixins
        addMixin(new HasStatusFlagsMixin(this));
        addMixin(new CommandableMixin(this, PropertyIdentifier.presentValue));
        addMixin(new WritablePropertyOutOfServiceMixin(this, PropertyIdentifier.reliability));
        addMixin(
                new ReadOnlyPropertyMixin(this, PropertyIdentifier.ackedTransitions, PropertyIdentifier.eventTimeStamps,
                        PropertyIdentifier.eventMessageTexts, PropertyIdentifier.interfaceValue));

        _supportCommandable(relinquishDefault);
        _supportValueSource();

        writePropertyInternal(PropertyIdentifier.presentValue, presentValue);
        writePropertyInternal(PropertyIdentifier.polarity, polarity);
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        writePropertyInternal(PropertyIdentifier.interfaceValue, new OptionalBinaryPV());

        addMixin(new StateChangeMixin(this));

        localDevice.addObject(this);
    }

    public BinaryOutputObject supportIntrinsicReporting(final int timeDelay, final int notificationClass,
            final BinaryPV feedbackValue, final EventTransitionBits eventEnable, final NotifyType notifyType,
            final int timeDelayNormal) {
        // Prepare the object with all of the properties that intrinsic reporting will need.
        writePropertyInternal(PropertyIdentifier.timeDelay, new UnsignedInteger(timeDelay));
        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(notificationClass));
        writePropertyInternal(PropertyIdentifier.feedbackValue, feedbackValue);
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);
        writePropertyInternal(PropertyIdentifier.timeDelayNormal, new UnsignedInteger(timeDelayNormal));
        writePropertyInternal(PropertyIdentifier.eventDetectionEnable, Boolean.TRUE);

        addMixin(new IntrinsicReportingMixin(this, new CommandFailureAlgo(), null, PropertyIdentifier.presentValue,
                new PropertyIdentifier[] { PropertyIdentifier.presentValue, PropertyIdentifier.feedbackValue }));

        return this;
    }

    public BinaryOutputObject supportStateText(final String inactive, final String active) {
        writePropertyInternal(PropertyIdentifier.inactiveText, new CharacterString(inactive));
        writePropertyInternal(PropertyIdentifier.activeText, new CharacterString(active));
        return this;
    }

    public BinaryOutputObject supportCovReporting() {
        _supportCovReporting(null, null);
        return this;
    }

    public BinaryOutputObject supportActiveTime(final boolean useFeedback) {
        if (useFeedback) {
            // Ensure that there is a feedback value.
            if (get(PropertyIdentifier.feedbackValue) == null) {
                throw new IllegalStateException("feedback-value not set");
            }
        }
        addMixin(new ActiveTimeMixin(this, useFeedback));
        return this;
    }

    public BinaryPV getPhysicalState() {
        final BinaryPV presentValue = get(PropertyIdentifier.presentValue);

        final Boolean outOfService = get(PropertyIdentifier.outOfService);
        if (outOfService.booleanValue())
            return presentValue;

        final Polarity polarity = get(PropertyIdentifier.polarity);
        if (polarity.equals(Polarity.normal))
            return presentValue;
        if (presentValue.equals(BinaryPV.active))
            return BinaryPV.inactive;
        return BinaryPV.active;
    }
}
