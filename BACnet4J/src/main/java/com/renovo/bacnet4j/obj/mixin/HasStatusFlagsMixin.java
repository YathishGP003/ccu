
package com.renovo.bacnet4j.obj.mixin;

import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.eventState;
import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.outOfService;
import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.reliability;
import static com.renovo.bacnet4j.type.enumerated.PropertyIdentifier.statusFlags;

import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.AbstractMixin;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.primitive.Boolean;

public class HasStatusFlagsMixin extends AbstractMixin {
    private boolean overridden;

    public HasStatusFlagsMixin(final BACnetObject bo) {
        super(bo);
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (statusFlags.equals(value.getPropertyIdentifier()))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        return false;
    }

    @Override
    public void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue, final Encodable newValue) {
        if (pid.isOneOf(eventState, reliability, outOfService))
            update();
    }

    private void update() {
        // Get the status flags object and associated values.
        final EventState eventState = get(PropertyIdentifier.eventState);
        final Reliability reliability = get(PropertyIdentifier.reliability);
        final Boolean outOfService = get(PropertyIdentifier.outOfService, Boolean.FALSE);

        // Update the status flags
        final StatusFlags statusFlags = new StatusFlags(//
                !EventState.normal.equals(eventState), //
                reliability == null ? false : !Reliability.noFaultDetected.equals(reliability), //
                overridden, //
                outOfService == null ? false : outOfService.booleanValue());
        writePropertyInternal(PropertyIdentifier.statusFlags, statusFlags);
    }

    public boolean isOverridden() {
        return overridden;
    }

    public void setOverridden(final boolean overridden) {
        this.overridden = overridden;
        update();
    }
}
