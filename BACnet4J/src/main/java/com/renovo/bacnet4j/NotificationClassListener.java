
package com.renovo.bacnet4j;

import com.renovo.bacnet4j.type.constructed.TimeStamp;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

/**
 * An internal (proprietary) mechanism for listening internally for event/alarm notifications via intrinsic
 * reporting. To use, implement this interface and add as a listener within a NotificationClassObject.
 *
 * @author Matthew
 */
public interface NotificationClassListener {
    /**
     * Calls to this method are made in internal threads that should not be blocked. If blocking is required, methods
     * such as LocalDevice.submit and LocalDevice.execute can be used to run code asynchronously.
     */
    void event(ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass,
            UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType,
            Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues);
}
