
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class EventState extends Enumerated {
    public static final EventState normal = new EventState(0);
    public static final EventState fault = new EventState(1);
    public static final EventState offnormal = new EventState(2);
    public static final EventState highLimit = new EventState(3);
    public static final EventState lowLimit = new EventState(4);
    public static final EventState lifeSafetyAlarm = new EventState(5);

    public boolean isOffNormal() {
        return isOneOf(offnormal, highLimit, lowLimit, lifeSafetyAlarm);
    }

    public int getTransitionIndex() {
        if (isOffNormal())
            return 1;
        if (equals(EventState.fault))
            return 2;
        return 3;
    }

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static EventState forId(final int id) {
        EventState e = (EventState) idMap.get(id);
        if (e == null)
            e = new EventState(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static EventState forName(final String name) {
        return (EventState) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EventState(final int value) {
        super(value);
    }

    public EventState(final ByteQueue queue) throws BACnetErrorException {
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
