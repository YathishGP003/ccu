
package com.renovo.bacnet4j.obj.mixin.event;

import java.util.function.Consumer;

import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.obj.mixin.event.faultAlgo.FaultAlgorithm;
import com.renovo.bacnet4j.type.Encodable;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.Boolean;

/**
 * Mixin class for intrinsic reporting.
 *
 * @author Matthew
 */
public class IntrinsicReportingMixin extends EventReportingMixin {
    // Configuration
    private final PropertyIdentifier monitoredProperty;
    private final PropertyIdentifier[] triggerProperties;

    public IntrinsicReportingMixin(final BACnetObject bo, final EventAlgorithm eventAlgo,
            final FaultAlgorithm faultAlgo, final PropertyIdentifier monitoredProperty,
            final PropertyIdentifier[] triggerProperties) {
        super(bo, eventAlgo, faultAlgo);

        bo.writePropertyInternal(PropertyIdentifier.reliabilityEvaluationInhibit, Boolean.FALSE);

        this.monitoredProperty = monitoredProperty;
        this.triggerProperties = triggerProperties;

        // Update the state with the current values in the object.
        for (final PropertyIdentifier pid : triggerProperties)
            afterWriteProperty(pid, null, get(pid));
    }

    public IntrinsicReportingMixin withPostNotificationAction(
            final Consumer<NotificationParameters> postNotificationAction) {
        setPostNotificationAction(postNotificationAction);
        return this;
    }

    @Override
    protected synchronized void afterWriteProperty(final PropertyIdentifier pid, final Encodable oldValue,
            final Encodable newValue) {
        super.afterWriteProperty(pid, oldValue, newValue);

        if (pid.isOneOf(triggerProperties)) {
            // Get the monitored value, in case this isn't it.
            final Encodable prev, curr;
            if (pid.equals(monitoredProperty)) {
                prev = oldValue;
                curr = newValue;
            } else {
                prev = null;
                curr = get(monitoredProperty);
            }

            // Check if there was a fault state transition.
            final boolean fault = executeFaultAlgo(prev, curr);
            if (!fault) {
                // Ensure there is no current fault.
                final Reliability reli = get(PropertyIdentifier.reliability);
                if (reli == null || reli.equals(Reliability.noFaultDetected))
                    // No fault detected. Run the event algorithm
                    executeEventAlgo();
            }
        }
    }

    @Override
    protected StateTransition evaluateEventState(final BACnetObject bo, final EventAlgorithm eventAlgo) {
        return eventAlgo.evaluateIntrinsicEventState(bo);
    }

    @Override
    protected EventType getEventType(final EventAlgorithm eventAlgo) {
        return eventAlgo.getEventType();
    }

    @Override
    protected boolean updateAckedTransitions() {
        return true;
    }

    @Override
    protected NotificationParameters getNotificationParameters(final EventState fromState, final EventState toState,
            final BACnetObject bo, final EventAlgorithm eventAlgo) {
        return eventAlgo.getIntrinsicNotificationParameters(fromState, toState, bo);
    }

    @Override
    protected Reliability evaluateFaultState(final Encodable oldMonitoredValue, final Encodable newMonitoredValue,
            final BACnetObject bo, final FaultAlgorithm faultAlgo) {
        return faultAlgo.evaluateIntrinsic(oldMonitoredValue, newMonitoredValue, bo);
    }

    @Override
    protected PropertyValue getEventEnrollmentMonitoredProperty(final PropertyIdentifier pid) {
        throw new RuntimeException("Should not be called because EventEnrollment does not support intrinsic reporting");
    }
}
