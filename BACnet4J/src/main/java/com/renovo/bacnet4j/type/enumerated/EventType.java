
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.eventParameter.ChangeOfDiscreteValue;
import com.renovo.bacnet4j.type.eventParameter.ChangeOfTimer;
import com.renovo.bacnet4j.type.notificationParameters.AccessEventNotif;
import com.renovo.bacnet4j.type.notificationParameters.BufferReadyNotif;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfBitStringNotif;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfCharacterStringNotif;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfLifeSafetyNotif;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfReliabilityNotif;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfStateNotif;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfStatusFlagsNotif;
import com.renovo.bacnet4j.type.notificationParameters.ChangeOfValueNotif;
import com.renovo.bacnet4j.type.notificationParameters.CommandFailureNotif;
import com.renovo.bacnet4j.type.notificationParameters.DoubleOutOfRangeNotif;
import com.renovo.bacnet4j.type.notificationParameters.ExtendedNotif;
import com.renovo.bacnet4j.type.notificationParameters.FloatingLimitNotif;
import com.renovo.bacnet4j.type.notificationParameters.OutOfRangeNotif;
import com.renovo.bacnet4j.type.notificationParameters.SignedOutOfRangeNotif;
import com.renovo.bacnet4j.type.notificationParameters.UnsignedOutOfRangeNotif;
import com.renovo.bacnet4j.type.notificationParameters.UnsignedRangeNotif;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class EventType extends Enumerated {
    public static final EventType changeOfBitstring = new EventType(ChangeOfBitStringNotif.TYPE_ID);
    public static final EventType changeOfState = new EventType(ChangeOfStateNotif.TYPE_ID);
    public static final EventType changeOfValue = new EventType(ChangeOfValueNotif.TYPE_ID);
    public static final EventType commandFailure = new EventType(CommandFailureNotif.TYPE_ID);
    public static final EventType floatingLimit = new EventType(FloatingLimitNotif.TYPE_ID);
    public static final EventType outOfRange = new EventType(OutOfRangeNotif.TYPE_ID);
    public static final EventType changeOfLifeSafety = new EventType(ChangeOfLifeSafetyNotif.TYPE_ID);
    public static final EventType extended = new EventType(ExtendedNotif.TYPE_ID);
    public static final EventType bufferReady = new EventType(BufferReadyNotif.TYPE_ID);
    public static final EventType unsignedRange = new EventType(UnsignedRangeNotif.TYPE_ID);
    public static final EventType accessEvent = new EventType(AccessEventNotif.TYPE_ID);
    public static final EventType doubleOutOfRange = new EventType(DoubleOutOfRangeNotif.TYPE_ID);
    public static final EventType signedOutOfRange = new EventType(SignedOutOfRangeNotif.TYPE_ID);
    public static final EventType unsignedOutOfRange = new EventType(UnsignedOutOfRangeNotif.TYPE_ID);
    public static final EventType changeOfCharacterstring = new EventType(ChangeOfCharacterStringNotif.TYPE_ID);
    public static final EventType changeOfStatusFlags = new EventType(ChangeOfStatusFlagsNotif.TYPE_ID);
    public static final EventType changeOfReliability = new EventType(ChangeOfReliabilityNotif.TYPE_ID);
    public static final EventType none = new EventType(20);
    public static final EventType changeOfDiscreteValue = new EventType(ChangeOfDiscreteValue.TYPE_ID);
    public static final EventType changeOfTimer = new EventType(ChangeOfTimer.TYPE_ID);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static EventType forId(final int id) {
        EventType e = (EventType) idMap.get(id);
        if (e == null)
            e = new EventType(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static EventType forName(final String name) {
        return (EventType) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EventType(final int value) {
        super(value);
    }

    public EventType(final ByteQueue queue) throws BACnetErrorException {
        super(queue);
    }

    /**
     * Returns a unmodifiable map.
     *
     * @return unmodifiable map
     */
    public static Map<Integer, String> getPrettyMap() {
        return Collections.unmodifiableMap(prettyMap);
    }
    
     /**
     * Returns a unmodifiable nameMap.
     *
     * @return unmodifiable map
     */
    public static Map<String, Enumerated> getNameMap() {
        return Collections.unmodifiableMap(nameMap);
    }
    
    @Override
    public String toString() {
        return super.toString(prettyMap);
    }
}
