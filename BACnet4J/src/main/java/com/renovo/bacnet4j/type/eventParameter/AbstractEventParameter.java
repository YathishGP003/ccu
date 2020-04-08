package com.renovo.bacnet4j.type.eventParameter;

import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.EventAlgorithm;
import com.renovo.bacnet4j.type.constructed.BaseType;
import com.renovo.bacnet4j.type.constructed.DeviceObjectPropertyReference;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;

abstract public class AbstractEventParameter extends BaseType {
    abstract public EventAlgorithm createEventAlgorithm();

    /**
     * Override as required to update event parameters following a notification.
     *
     * @param notifParams
     */
    public void postNotification(final NotificationParameters notifParams) {
        // no op
    }

    /**
     * Override as required to return a reference property if the subclass has one.
     *
     * @return the property reference.
     */
    public DeviceObjectPropertyReference getReference() {
        return null;
    }
}
