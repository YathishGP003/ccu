
package com.renovo.bacnet4j.obj.mixin.event.eventAlgo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.mixin.event.StateTransition;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.type.constructed.PropertyStates;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.renovo.bacnet4j.type.eventParameter.ChangeOfState;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfStateNotif;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Implements change of state event algorithm.
 *
 * @author Matthew
 */
public class ChangeOfStateAlgo extends EventAlgorithm {
    static final Logger LOG = LoggerFactory.getLogger(ChangeOfStateAlgo.class);

    private final PropertyIdentifier monitoredValueProperty;
    private final PropertyIdentifier alarmValuesProperty;

    private Encodable lastValue;
    private Encodable lastValueCausingTransition;

    public ChangeOfStateAlgo() {
        this(null, null);
    }

    public ChangeOfStateAlgo(final PropertyIdentifier monitoredValueProperty,
            final PropertyIdentifier alarmValuesProperty) {
        this.monitoredValueProperty = monitoredValueProperty;
        this.alarmValuesProperty = alarmValuesProperty;
    }

    @Override
    public EventType getEventType() {
        return EventType.changeOfState;
    }

    @Override
    public PropertyIdentifier[] getAdditionalMonitoredProperties() {
        return new PropertyIdentifier[] { PropertyIdentifier.statusFlags };
    }

    @Override
    public StateTransition evaluateIntrinsicEventState(final BACnetObject bo) {
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                bo.get(monitoredValueProperty), //
                bo.get(alarmValuesProperty), //
                bo.get(PropertyIdentifier.timeDelay), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    @Override
    public StateTransition evaluateAlgorithmicEventState(final BACnetObject bo, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        final ChangeOfState p = (ChangeOfState) parameters;
        return evaluateEventState( //
                bo.get(PropertyIdentifier.eventState), //
                monitoredValue, //
                p.getListOfValues(), //
                p.getTimeDelay(), //
                bo.get(PropertyIdentifier.timeDelayNormal));
    }

    private StateTransition evaluateEventState(final EventState currentState, final Encodable monitoredValue,
            final Encodable alarmValues, final UnsignedInteger timeDelay, UnsignedInteger timeDelayNormal) {
        if (timeDelayNormal == null)
            timeDelayNormal = timeDelay;

        LOG.debug("Current state: {}, monitored value: {}, alarm values: {}", currentState, monitoredValue,
                alarmValues);

        final boolean isAlarmValue = isAlarmValue(monitoredValue, alarmValues);
        lastValue = monitoredValue;

        // (a)
        if (currentState.equals(EventState.normal) && isAlarmValue)
            return new StateTransition(EventState.offnormal, timeDelay);

        // (b)
        if (currentState.equals(EventState.offnormal) && !isAlarmValue)
            return new StateTransition(EventState.normal, timeDelayNormal);

        // (c)
        if (currentState.equals(EventState.offnormal) && isAlarmValue
                && !monitoredValue.equals(lastValueCausingTransition))
            return new StateTransition(EventState.offnormal, timeDelay);

        return null;
    }

    @Override
    public void stateChangeNotify(final EventState toState) {
        lastValueCausingTransition = lastValue;
    }

    private static boolean isAlarmValue(final Encodable monitoredValue, final Encodable alarmValue) {
        // The alarm value could be:
        // 1) A BinaryPV in the case of Binary intrinsic
        // 2) A list of unsigned in the case of Multistate intrinsic
        // 3) A list of PropertyStates for algorithmic
        if (alarmValue instanceof SequenceOf) {
            @SuppressWarnings("unchecked")
            final SequenceOf<Encodable> alarmValues = (SequenceOf<Encodable>) alarmValue;
            // Case 2)
            if (alarmValues.contains(monitoredValue))
                return true;

            // Case 3)
            for (final Encodable e : alarmValues) {
                if (e instanceof PropertyStates) {
                    if (((PropertyStates) e).getState().equals(monitoredValue))
                        return true;
                }
            }

            return false;
        }
        // Case 1)
        return alarmValue.equals(monitoredValue);
    }

    @Override
    public NotificationParameters getIntrinsicNotificationParameters(final EventState fromState,
            final EventState toState, final BACnetObject bo) {
        return getNotificationParameters(bo.get(PropertyIdentifier.statusFlags), bo.get(monitoredValueProperty));
    }

    @Override
    public NotificationParameters getAlgorithmicNotificationParameters(final BACnetObject bo,
            final EventState fromState, final EventState toState, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        return getNotificationParameters(
                (StatusFlags) additionalValues
                        .get(new ObjectPropertyReference(monitoredObjectReference, PropertyIdentifier.statusFlags)),
                monitoredValue);
    }

    private static NotificationParameters getNotificationParameters(StatusFlags statusFlags,
            final Encodable monitoredValue) {
        if (statusFlags == null)
            statusFlags = new StatusFlags(false, false, false, false);

        return new NotificationParameters(new ChangeOfStateNotif(new PropertyStates(monitoredValue), statusFlags));
    }
}
