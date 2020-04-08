
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
public class DoorStatus extends Enumerated {
    public static final DoorStatus closed = new DoorStatus(0);
    public static final DoorStatus open = new DoorStatus(1);
    public static final DoorStatus unknown = new DoorStatus(2);
    public static final DoorStatus doorFault = new DoorStatus(3);
    public static final DoorStatus unused = new DoorStatus(4);
    public static final DoorStatus none = new DoorStatus(4);
    public static final DoorStatus closing = new DoorStatus(4);
    public static final DoorStatus opening = new DoorStatus(4);
    public static final DoorStatus safetyLocked = new DoorStatus(4);
    public static final DoorStatus limitedOpened = new DoorStatus(4);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static DoorStatus forId(final int id) {
        DoorStatus e = (DoorStatus) idMap.get(id);
        if (e == null)
            e = new DoorStatus(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static DoorStatus forName(final String name) {
        return (DoorStatus) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private DoorStatus(final int value) {
        super(value);
    }

    public DoorStatus(final ByteQueue queue) throws BACnetErrorException {
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
