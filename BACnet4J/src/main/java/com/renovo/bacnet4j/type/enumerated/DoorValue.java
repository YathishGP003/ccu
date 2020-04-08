
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

/**
 * @author Suresh Kumar
 */
public class DoorValue extends Enumerated {
    public static final DoorValue lock = new DoorValue(0);
    public static final DoorValue unlock = new DoorValue(1);
    public static final DoorValue pulseUnlock = new DoorValue(2);
    public static final DoorValue extendedPulseUnlock = new DoorValue(3);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static DoorValue forId(final int id) {
        DoorValue e = (DoorValue) idMap.get(id);
        if (e == null)
            e = new DoorValue(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static DoorValue forName(final String name) {
        return (DoorValue) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private DoorValue(final int value) {
        super(value);
    }

    public DoorValue(final ByteQueue queue) throws BACnetErrorException {
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
