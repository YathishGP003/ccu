
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class TimerTransition extends Enumerated {
    public static final TimerTransition none = new TimerTransition(0);
    public static final TimerTransition idleToRunning = new TimerTransition(1);
    public static final TimerTransition runningToIdle = new TimerTransition(2);
    public static final TimerTransition runningToRunning = new TimerTransition(3);
    public static final TimerTransition runningToExpired = new TimerTransition(4);
    public static final TimerTransition forcedToExpire = new TimerTransition(5);
    public static final TimerTransition expiredToIdle = new TimerTransition(6);
    public static final TimerTransition expiredToRunning = new TimerTransition(7);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static TimerTransition forId(final int id) {
        TimerTransition e = (TimerTransition) idMap.get(id);
        if (e == null)
            e = new TimerTransition(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static TimerTransition forName(final String name) {
        return (TimerTransition) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private TimerTransition(final int value) {
        super(value);
    }

    public TimerTransition(final ByteQueue queue) throws BACnetErrorException {
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
