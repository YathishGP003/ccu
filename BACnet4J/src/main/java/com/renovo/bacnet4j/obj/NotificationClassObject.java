
package com.renovo.bacnet4j.obj;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.renovo.bacnet4j.LocalDevice;
import com.renovo.bacnet4j.NotificationClassListener;
import com.renovo.bacnet4j.exception.BACnetServiceException;
import com.renovo.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.renovo.bacnet4j.obj.mixin.event.IntrinsicReportingMixin;
import com.renovo.bacnet4j.obj.mixin.event.eventAlgo.NoneAlgo;
import com.renovo.bacnet4j.type.constructed.BACnetArray;
import com.renovo.bacnet4j.type.constructed.Destination;
import com.renovo.bacnet4j.type.constructed.EventTransitionBits;
import com.renovo.bacnet4j.type.constructed.PropertyValue;
import com.renovo.bacnet4j.type.constructed.SequenceOf;
import com.renovo.bacnet4j.type.constructed.StatusFlags;
import com.renovo.bacnet4j.type.constructed.TimeStamp;
import com.renovo.bacnet4j.type.constructed.ValueSource;
import com.renovo.bacnet4j.type.enumerated.ErrorClass;
import com.renovo.bacnet4j.type.enumerated.ErrorCode;
import com.renovo.bacnet4j.type.enumerated.EventState;
import com.renovo.bacnet4j.type.enumerated.EventType;
import com.renovo.bacnet4j.type.enumerated.NotifyType;
import com.renovo.bacnet4j.type.enumerated.ObjectType;
import com.renovo.bacnet4j.type.enumerated.PropertyIdentifier;
import com.renovo.bacnet4j.type.enumerated.Reliability;
import com.renovo.bacnet4j.type.notificationParameters.NotificationParameters;
import com.renovo.bacnet4j.type.primitive.Boolean;
import com.renovo.bacnet4j.type.primitive.CharacterString;
import com.renovo.bacnet4j.type.primitive.ObjectIdentifier;
import com.renovo.bacnet4j.type.primitive.UnsignedInteger;

import sun.rmi.runtime.Log;

public class NotificationClassObject extends BACnetObject {
    // CreateObject constructor
    public static NotificationClassObject create(final LocalDevice localDevice, final int instanceNumber)
            throws BACnetServiceException {
        return new NotificationClassObject(localDevice, instanceNumber,
                ObjectType.notificationClass.toString() + " " + instanceNumber, 20, 10, 30,
                new EventTransitionBits(false, false, false))
                        .supportIntrinsicReporting(new EventTransitionBits(false, false, false), NotifyType.event);
    }

    private final List<NotificationClassListener> eventListeners = new CopyOnWriteArrayList<>();

    public NotificationClassObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final int toOffnormalPriority, final int toFaultPriority, final int toNormalPriority,
            final EventTransitionBits ackRequired) throws BACnetServiceException {
        this(localDevice, instanceNumber, name, new BACnetArray<>(new UnsignedInteger(toOffnormalPriority),
                new UnsignedInteger(toFaultPriority), new UnsignedInteger(toNormalPriority)), ackRequired);

        writePropertyInternal(PropertyIdentifier.recipientList, new SequenceOf<Destination>());
        System.out.println("Bacnet Notification:"+instanceNumber);
    }

    public NotificationClassObject(final LocalDevice localDevice, final int instanceNumber, final String name,
            final BACnetArray<UnsignedInteger> priority, final EventTransitionBits ackRequired)
            throws BACnetServiceException {
        super(localDevice, ObjectType.notificationClass, instanceNumber, name);

        writePropertyInternal(PropertyIdentifier.notificationClass, new UnsignedInteger(instanceNumber));
        writePropertyInternal(PropertyIdentifier.priority, priority);
        writePropertyInternal(PropertyIdentifier.ackRequired, ackRequired);
        writePropertyInternal(PropertyIdentifier.reliability, Reliability.noFaultDetected);
        //writePropertyInternal(PropertyIdentifier.statusFlags, new StatusFlags(false, false, false, false));

        addMixin(new HasStatusFlagsMixin(this));

        localDevice.addObject(this);
    }

    public NotificationClassObject supportIntrinsicReporting(final EventTransitionBits eventEnable,
            final NotifyType notifyType) {
        // Prepare the object with all of the properties that intrinsic reporting will need.
        // User-defined properties
        writePropertyInternal(PropertyIdentifier.eventEnable, eventEnable);
        writePropertyInternal(PropertyIdentifier.eventState, EventState.normal);
        writePropertyInternal(PropertyIdentifier.notifyType, notifyType);

        // Now add the mixin.
        addMixin(new IntrinsicReportingMixin(this, new NoneAlgo(), null, null, new PropertyIdentifier[0]));

        return this;
    }

    @Override
    protected boolean validateProperty(final ValueSource valueSource, final PropertyValue value)
            throws BACnetServiceException {
        if (PropertyIdentifier.priority.equals(value.getPropertyIdentifier())) {
            final SequenceOf<UnsignedInteger> priority = value.getValue();
            if (priority.getCount() != 3)
                throw new BACnetServiceException(ErrorClass.property, ErrorCode.valueOutOfRange);
        }

        return false;
    }

    public void addEventListener(final NotificationClassListener l) {
        eventListeners.add(l);
    }

    public void removeEventListener(final NotificationClassListener l) {
        eventListeners.remove(l);
    }

    public void fireEventNotification(final ObjectIdentifier eventObjectIdentifier, final TimeStamp timeStamp,
            final UnsignedInteger notificationClass, final UnsignedInteger priority, final EventType eventType,
            final CharacterString messageText, final NotifyType notifyType, final Boolean ackRequired,
            final EventState fromState, final EventState toState, final NotificationParameters eventValues) {
        for (final NotificationClassListener l : eventListeners)
            l.event(eventObjectIdentifier, timeStamp, notificationClass, priority, eventType, messageText, notifyType,
                    ackRequired, fromState, toState, eventValues);
    }
}
