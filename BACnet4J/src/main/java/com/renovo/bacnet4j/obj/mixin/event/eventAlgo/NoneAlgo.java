
package com.renovo.bacnet4j.obj.mixin.event.eventAlgo;

import java.util.Map;

import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.mixin.event.StateTransition;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.ObjectPropertyReference;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.eventParameter.AbstractEventParameter;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;

/**
 * Implements change of state event algorithm.
 *
 * @author Matthew
 */
public class NoneAlgo extends EventAlgorithm {
    @Override
    public EventType getEventType() {
        return EventType.none;
    }

    @Override
    public PropertyIdentifier[] getAdditionalMonitoredProperties() {
        return null;
    }

    @Override
    public StateTransition evaluateIntrinsicEventState(final BACnetObject bo) {
        return null;
    }

    @Override
    public NotificationParameters getIntrinsicNotificationParameters(final EventState fromState,
            final EventState toState, final BACnetObject bo) {
        return null;
    }

    @Override
    public StateTransition evaluateAlgorithmicEventState(final BACnetObject bo, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        return null;
    }

    @Override
    public NotificationParameters getAlgorithmicNotificationParameters(final BACnetObject bo,
            final EventState fromState, final EventState toState, final Encodable monitoredValue,
            final ObjectIdentifier monitoredObjectReference,
            final Map<ObjectPropertyReference, Encodable> additionalValues, final AbstractEventParameter parameters) {
        return null;
    }
}
