
package com.renovo.bacnet4j.event;

import com.renovo.bacnet4j.RemoteDevice;
import com.renovo.bacnet4j.RemoteObject;
import com.renovo.bacnet4j.apdu.APDU;
import com.renovo.bacnet4j.apdu.ComplexACK;
import com.renovo.bacnet4j.obj.BACnetObject;
import com.renovo.bacnet4j.service.Service;
import com.renovo.bacnet4j.service.acknowledgement.AcknowledgementService;
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
 * A default class for easy implementation of the DeviceEventListener interface. Instead of having to implement all of
 * the defined methods, listener classes can override this and only implement the desired methods.
 *
 * @author Suresh Kumar
 */
public class DeviceEventAdapter implements DeviceEventListener {
    @Override
    public void listenerException(final Throwable e) {
        // Override as required
        e.printStackTrace();
    }

    @Override
    public boolean allowPropertyWrite(final Address from, final BACnetObject obj, final PropertyValue pv) {
        return true;
    }

    @Override
    public void iAmReceived(final RemoteDevice d) {
        // Override as required
    }

    @Override
    public void propertyWritten(final Address from, final BACnetObject obj, final PropertyValue pv) {
        // Override as required
    }

    @Override
    public void iHaveReceived(final RemoteDevice d, final RemoteObject o) {
        // Override as required
    }

    @Override
    public void covNotificationReceived(final UnsignedInteger subscriberProcessIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier monitoredObjectIdentifier,
            final UnsignedInteger timeRemaining, final SequenceOf<PropertyValue> listOfValues) {
        // Override as required
    }

    @Override
    public void eventNotificationReceived(final UnsignedInteger processIdentifier,
            final ObjectIdentifier initiatingDeviceIdentifier, final ObjectIdentifier eventObjectIdentifier,
            final TimeStamp timeStamp, final UnsignedInteger notificationClass, final UnsignedInteger priority,
            final EventType eventType, final CharacterString messageText, final NotifyType notifyType,
            final Boolean ackRequired, final EventState fromState, final EventState toState,
            final NotificationParameters eventValues) {
        // Override as required
    }

    @Override
    public void textMessageReceived(final ObjectIdentifier textMessageSourceDevice, final Choice messageClass,
            final MessagePriority messagePriority, final CharacterString message) {
        // Override as required
    }

    @Override
    public void synchronizeTime(final Address from, final DateTime dateTime, final boolean utc) {
        // Override as required
    }

    @Override
    public void requestReceived(final Address from, final Service service) {
        // Override as required
    }
}
