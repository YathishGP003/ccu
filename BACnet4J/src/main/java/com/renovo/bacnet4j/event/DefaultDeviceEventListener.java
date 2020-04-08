/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
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
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Default implementation for all methods is to do nothing
 *   useful to create functional interfaces that extend this
 * 
 * @author Terry Packer
 *
 */
public interface DefaultDeviceEventListener extends DeviceEventListener {
    @Override
    default void listenerException(Throwable e) {}

    @Override
    default void iAmReceived(RemoteDevice d) {}

    @Override
    default boolean allowPropertyWrite(Address from, BACnetObject obj, PropertyValue pv) { return true; }

    @Override
    default void propertyWritten(Address from, BACnetObject obj, PropertyValue pv) {}

    @Override
    default void iHaveReceived(RemoteDevice d, RemoteObject o) {}

    @Override
    default void covNotificationReceived(UnsignedInteger subscriberProcessIdentifier,
                                         ObjectIdentifier initiatingDeviceIdentifier, ObjectIdentifier monitoredObjectIdentifier,
                                         UnsignedInteger timeRemaining, SequenceOf<PropertyValue> listOfValues) {}

    default void eventNotificationReceived(UnsignedInteger processIdentifier, ObjectIdentifier initiatingDeviceIdentifier,
                                           ObjectIdentifier eventObjectIdentifier, TimeStamp timeStamp, UnsignedInteger notificationClass,
                                           UnsignedInteger priority, EventType eventType, CharacterString messageText, NotifyType notifyType,
                                           Boolean ackRequired, EventState fromState, EventState toState, NotificationParameters eventValues) {}

    @Override
    default void textMessageReceived(ObjectIdentifier textMessageSourceDevice, Choice messageClass,
                                     MessagePriority messagePriority, CharacterString message) {}

    @Override
    default void synchronizeTime(Address from, DateTime dateTime, boolean utc) {}

    @Override
    default void requestReceived(Address from, Service service) {}
}
