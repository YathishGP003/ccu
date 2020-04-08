
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
 * Base class for implementations of event algorithms from 13.3
 *
 * @author Matthew
 */
abstract public class EventAlgorithm {
    abstract public EventType getEventType();

    /**
     * Evaluation of state transition in intrinsic reporting
     *
     * @param bo
     *            the object in which the intrinsic monitoring is occurring.
     * @return the state transition
     */
    abstract public StateTransition evaluateIntrinsicEventState(BACnetObject bo);

    /**
     * Return the notification parameters for intrinsic reporting.
     *
     * @param fromState
     * @param toState
     * @param bo
     *            the object in which the intrinsic monitoring is occurring.
     * @return
     */
    abstract public NotificationParameters getIntrinsicNotificationParameters(EventState fromState, EventState toState,
            BACnetObject bo);

    /**
     * Additionally monitored properties for algorithmic reporting. See Table 12-15.1
     *
     * @return
     */
    abstract public PropertyIdentifier[] getAdditionalMonitoredProperties();

    /**
     * Evaluation of state transition in algorithmic reporting
     *
     * @param ee
     *            the event enrollment object
     * @param monitoredValue
     * @param parameters
     * @return
     */
    abstract public StateTransition evaluateAlgorithmicEventState(BACnetObject ee, Encodable monitoredValue,
            ObjectIdentifier monitoredObjectReference, Map<ObjectPropertyReference, Encodable> additionalValues,
            AbstractEventParameter parameters);

    /**
     * Return the notification parameters for algorithmic reporting.
     *
     * @param ee
     *            the event enrollment object
     * @param fromState
     * @param toState
     * @param monitoredValue
     * @param additionalValues
     *            the additional parameters values as per getAdditionalMonitoredProperties.
     * @param parameters
     * @return
     */
    abstract public NotificationParameters getAlgorithmicNotificationParameters(BACnetObject ee, EventState fromState,
            EventState toState, Encodable monitoredValue, ObjectIdentifier monitoredObjectReference,
            Map<ObjectPropertyReference, Encodable> additionalValues, AbstractEventParameter parameters);

    /**
     * Override as required to handle actual state changes.
     *
     * @param toState
     */
    public void stateChangeNotify(final EventState toState) {
        // no op
    }
}
