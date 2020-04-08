
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LifeSafetyMode extends Enumerated {
    public static final LifeSafetyMode off = new LifeSafetyMode(0);
    public static final LifeSafetyMode on = new LifeSafetyMode(1);
    public static final LifeSafetyMode test = new LifeSafetyMode(2);
    public static final LifeSafetyMode manned = new LifeSafetyMode(3);
    public static final LifeSafetyMode unmanned = new LifeSafetyMode(4);
    public static final LifeSafetyMode armed = new LifeSafetyMode(5);
    public static final LifeSafetyMode disarmed = new LifeSafetyMode(6);
    public static final LifeSafetyMode prearmed = new LifeSafetyMode(7);
    public static final LifeSafetyMode slow = new LifeSafetyMode(8);
    public static final LifeSafetyMode fast = new LifeSafetyMode(9);
    public static final LifeSafetyMode disconnected = new LifeSafetyMode(10);
    public static final LifeSafetyMode enabled = new LifeSafetyMode(11);
    public static final LifeSafetyMode disabled = new LifeSafetyMode(12);
    public static final LifeSafetyMode automaticReleaseDisabled = new LifeSafetyMode(13);
    public static final LifeSafetyMode defaultMode = new LifeSafetyMode(14);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LifeSafetyMode forId(final int id) {
        LifeSafetyMode e = (LifeSafetyMode) idMap.get(id);
        if (e == null)
            e = new LifeSafetyMode(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LifeSafetyMode forName(final String name) {
        return (LifeSafetyMode) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LifeSafetyMode(final int value) {
        super(value);
    }

    public LifeSafetyMode(final ByteQueue queue) throws BACnetErrorException {
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
