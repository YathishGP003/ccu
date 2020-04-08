
package com.renovo.bacnet4j.obj.mixin.event.eventAlgo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.exception.BACnetRuntimeException;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.mixin.event.StateTransition;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.LimitEnable;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.renovo.bacnet4j.type.eventParameter.OutOfRange;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.Real;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

public class OutOfRangeAlgo extends EventAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(OutOfRangeAlgo.class);

    private final LimitEnable algorithmicLimitEnable = new LimitEnable(true, true);

    @Override
    public EventType getEventType() {
        return EventType.outOfRange;
    }

    @Override
    public PropertyIdentifier[] getAdditionalMonitoredProperties() {
        return new PropertyIdentifier[] { PropertyIdentifier.statusFlags };
    }

    @Override
    public StateTransition evaluateIntrinsicEventState(final BACnetObject bo) {
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                bo.get(PropertyIdentifier.presentValue), //
                bo.get(PropertyIdentifier.highLimit), //
                bo.get(PropertyIdentifier.lowLimit), //
                bo.get(PropertyIdentifier.deadband), //
                bo.get(PropertyIdentifier.timeDelay), //
                bo.get(PropertyIdentifier.limitEnable), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    @Override
    public StateTransition evaluateAlgorithmicEventState(final BACnetObject bo, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final OutOfRange p = (OutOfRange) parameters;
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                (Real) monitoredValue, //
                p.getHighLimit(), //
                p.getLowLimit(), //
                p.getDeadband(), //
                p.getTimeDelay(), //
                algorithmicLimitEnable, // 13.3.6 pLimitEnable - default to both on.
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    private static StateTransition evaluateEventState(final EventState currentState, final Real monitoredValue,
            final Real highLimit, final Real lowLimit, final Real deadband, final UnsignedInteger timeDelay,
            final LimitEnable limitEnable, UnsignedInteger timeDelayNormal) {
        final float monitoredValueFloat = monitoredValue.floatValue();
        final float highLimitFloat = highLimit.floatValue();
        final float lowLimitFloat = lowLimit.floatValue();
        final float deadbandFloat = deadband.floatValue();

        if (timeDelayNormal == null)
            timeDelayNormal = timeDelay;

        LOG.debug("state={}, pv={}, highLimit={}, lowLimit={}, deadband={}", currentState, monitoredValueFloat,
                highLimitFloat, lowLimitFloat, deadbandFloat);

        if (currentState.equals(EventState.normal) && limitEnable.isHighLimitEnable()
                && monitoredValueFloat > highLimitFloat)
            return new StateTransition(EventState.highLimit, timeDelay);

        if (currentState.equals(EventState.normal) && limitEnable.isLowLimitEnable()
                && monitoredValueFloat < lowLimitFloat)
            return new StateTransition(EventState.lowLimit, timeDelay);

        if (currentState.equals(EventState.highLimit) && !limitEnable.isHighLimitEnable())
            return new StateTransition(EventState.normal, null);

        if (currentState.equals(EventState.highLimit) && limitEnable.isLowLimitEnable()
                && monitoredValueFloat < lowLimitFloat)
            return new StateTransition(EventState.lowLimit, timeDelay);

        if (currentState.equals(EventState.highLimit) && monitoredValueFloat < highLimitFloat - deadbandFloat)
            return new StateTransition(EventState.normal, timeDelayNormal);

        if (currentState.equals(EventState.lowLimit) && !limitEnable.isLowLimitEnable())
            return new StateTransition(EventState.normal, null);

        if (currentState.equals(EventState.lowLimit) && limitEnable.isHighLimitEnable()
                && monitoredValueFloat > highLimitFloat)
            return new StateTransition(EventState.highLimit, timeDelay);

        if (currentState.equals(EventState.lowLimit) && monitoredValueFloat > lowLimitFloat + deadbandFloat)
            return new StateTransition(EventState.normal, timeDelayNormal);

        return null;
    }

    @Override
    public NotificationParameters getIntrinsicNotificationParameters(final EventState fromState,
            final EventState toState, final BACnetObject bo) {
        return getNotificationParameters(fromState, toState, //
                bo.get(PropertyIdentifier.lowLimit), //
                bo.get(PropertyIdentifier.highLimit), //
                bo.get(PropertyIdentifier.presentValue), //
                bo.get(PropertyIdentifier.statusFlags), //
                bo.get(PropertyIdentifier.deadband));
    }

    @Override
    public NotificationParameters getAlgorithmicNotificationParameters(final BACnetObject bo,
            final EventState fromState, final EventState toState, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final OutOfRange p = (OutOfRange) parameters;
        return getNotificationParameters(fromState, toState, //
                p.getLowLimit(), //
                p.getHighLimit(), //
                (Real) monitoredValue, //
                (StatusFlags) additionalValues
                        .get(new ObjectPropertyReference(monitoredObjectReference, PropertyIdentifier.statusFlags)), //
                p.getDeadband());
    }

    private static NotificationParameters getNotificationParameters(final EventState fromState,
            final EventState toState, final Real lowLimit, final Real highLimit, final Real presentValue,
            final StatusFlags statusFlags, final Real deadband) {
        Real exceededLimit;
        if (EventState.lowLimit.equals(toState) //
                || EventState.lowLimit.equals(fromState) && EventState.normal.equals(toState))
            exceededLimit = lowLimit;
        else if (EventState.highLimit.equals(toState) //
                || EventState.highLimit.equals(fromState) && EventState.normal.equals(toState))
            exceededLimit = highLimit;
        else
            throw new BACnetRuntimeException("Invalid state transition: " + toState + " to " + fromState);

        LOG.debug("Notification parameters: from={}, to={}, exceededLimit={}", fromState, toState, exceededLimit);

        return new NotificationParameters(new OutOfRangeNotif(presentValue, statusFlags, deadband, exceededLimit));
    }
}
