
package com.renovo.bacnet4j.event;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.RemoteObject;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.Service;
import com.renovo.bacnet4j.type.constructed.Address;
import com.renovo.bacnet4j.type.constructed.Choice;
import com.renovo.bacnet4j.type.constructed.DateTime;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.TimeStamp;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.MessagePriority;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Class to handle various events that occur on the local device. This class accepts 0 to many listeners, and dispatches
 * notifications synchronously.
 *
 * @author Suresh Kumar
 */
public class DeviceEventHandler {
    final ConcurrentLinkedQueue<DeviceEventListener> listeners = new ConcurrentLinkedQueue<>();

    //
    //
    // Listener management
    //
    public void addListener(final DeviceEventListener l) {
        listeners.add(l);
    }

    public void removeListener(final DeviceEventListener l) {
        listeners.remove(l);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    //
    //
    // Checks and notifications
    //
    public boolean checkAllowPropertyWrite(final Address from, final BACnetObject obj, final PropertyValue pv) {
        for (final DeviceEventListener l : listeners) {
            try {
                if (!l.allowPropertyWrite(from, obj, pv))
                    return false;
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
        return true;
    }

    public void fireIAmReceived(final RemoteDevice d) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.iAmReceived(d);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void propertyWritten(final Address from, final BACnetObject obj, final PropertyValue pv) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.propertyWritten(from, obj, pv);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireIHaveReceived(final RemoteDevice d, final RemoteObject o) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.iHaveReceived(d, o);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireCovNotification(final UnsignedInteger subscriberProcessIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier monitoredObjectIdentifier,
            final UnsignedInteger timeRemaining, final SequenceOf<PropertyValue> listOfValues) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.covNotificationReceived(subscriberProcessIdentifier, initiatingDeviceIdentifier,
                        monitoredObjectIdentifier, timeRemaining, listOfValues);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireEventNotification(final UnsignedInteger processIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier eventObjectIdentifier,
            final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final EventState toState,
            final NotificationParameters eventValues) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.eventNotificationReceived(processIdentifier, initiatingDeviceIdentifier, eventObjectIdentifier,
                        timeStamp, notificationClass, priority, eventType, messageText, notifyType, ackRequired,
                        fromState, toState, eventValues);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void fireTextMessage(final ObjectIdentifier textMessageSourceDevice, final Choice messageClass,
            final MessagePriority messagePriority, final CharacterString message) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.textMessageReceived(textMessageSourceDevice, messageClass, messagePriority, message);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.synchronizeTime(from, dateTime, utc);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void requestReceived(final Address from, final Service service) {
        for (final DeviceEventListener l : listeners) {
            try {
                l.requestReceived(from, service);
            } catch (final Exception e) {
                handleException(l, e);
            }
        }
    }

    public void handleException(final Exception e) {
        for (final DeviceEventListener l : listeners)
            handleException(l, e);
    }

    private static void handleException(final DeviceEventListener l, final Exception e) {
        try {
            l.listenerException(e);
        } catch (@SuppressWarnings("unused") final Exception e1) {
            // no op
        }
    }
}
