
package com.renovo.bacnet4j.event;

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
 * There are two versions of each method. The "regular" version runs in a worker thread so that client code need not
 * be concerned with completing its processing in any timely manner. The "sync" version runs within the dispatching
 * thread, and so MUST return as quickly as possible. In particular, overrides of the sync methods MUST NOT send
 * requests.
 *
 * @author Matthew
 */
public interface DeviceEventListener {
    /**
     * Notification of an exception while calling a listener method.
     */
    void listenerException(Throwable e);

    /**
     * Notification of receipt of an IAm message.
     *
     * @param d
     */
    void iAmReceived(RemoteDevice d);

    /**
     * Allow a listener to veto an attempt by another device to write a property in a local object.
     *
     * @param from
     * @param obj
     * @param pv
     * @return true if the write should be allowed.
     */
    boolean allowPropertyWrite(Address from, BACnetObject obj, PropertyValue pv);

    /**
     * Notification that a property of a local object was written by another device.
     *
     * @param from
     * @param obj
     * @param pv
     */
    void propertyWritten(Address from, BACnetObject obj, PropertyValue pv);

    /**
     * Notification of receipt of an IHave message.
     *
     * @param d
     * @param o
     */
    void iHaveReceived(RemoteDevice d, RemoteObject o);

    /**
     * Notification of either an UnconfirmedCovNotificationRequest or a ConfirmedCovNotificationRequest. The latter will
     * be automatically confirmed by the service handler.
     *
     * @param subscriberProcessIdentifier
     * @param initiatingDevice
     * @param monitoredObjectIdentifier
     * @param timeRemaining
     * @param listOfValues
     */
    void covNotificationReceived(UnsignedInteger subscriberProcessIdentifier,
            ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier monitoredObjectIdentifier,
            UnsignedInteger timeRemaining, SequenceOf<PropertyValue> listOfValues);

    /**
     * Notification of either an UnconfirmedEventNotificationRequest or a ConfirmedEventNotificationRequest. The latter
     * will be automatically confirmed by the service handler.
     *
     * @param processIdentifier
     * @param initiatingDevice
     * @param eventObjectIdentifier
     * @param timeStamp
     * @param notificationClass
     * @param priority
     * @param eventType
     * @param messageText
     * @param notifyType
     * @param ackRequired
     * @param fromState
     * @param toState
     * @param eventValues
     */
    void eventNotificationReceived(UnsignedInteger processIdentifier, ObjectIdentifier initiatingDeviceIdentifier,
            ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass,
            UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType,
            Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues);

    /**
     * Notification of either an UnconfirmedTextMessageRequest or a ConfirmedTextMessageRequest. The latter will be
     * automatically confirmed by the service handler.
     *
     * @param textMessageSourceDevice
     * @param messageClass
     * @param messagePriority
     * @param message
     */
    void textMessageReceived(ObjectIdentifier textMessageSourceDevice, Choice messageClass,
            MessagePriority messagePriority, CharacterString message);

    /**
     * Notification that the device should synchronize its time to the given date/time value. The local device has
     * already been checked at this point to ensure that it supports time synchronization.
     *
     * @param from
     * @param dateTime
     * @param utc
     *            true if a UTCTimeSynchronizationRequest was sent, false if TimeSynchronizationRequest.
     */
    void synchronizeTime(Address from, DateTime dateTime, boolean utc);

    /**
     * Notification that a service was received and from where.
     *
     * @param from
     * @param confirmed
     * @param serviceId
     */
    void requestReceived(Address from, Service service);
}