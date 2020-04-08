
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LiftCarDirection extends Enumerated {
    public static final LiftCarDirection unknown = new LiftCarDirection(0);
    public static final LiftCarDirection none = new LiftCarDirection(1);
    public static final LiftCarDirection stopped = new LiftCarDirection(2);
    public static final LiftCarDirection up = new LiftCarDirection(3);
    public static final LiftCarDirection down = new LiftCarDirection(4);
    public static final LiftCarDirection upAndDown = new LiftCarDirection(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LiftCarDirection forId(final int id) {
        LiftCarDirection e = (LiftCarDirection) idMap.get(id);
        if (e == null)
            e = new LiftCarDirection(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LiftCarDirection forName(final String name) {
        return (LiftCarDirection) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LiftCarDirection(final int value) {
        super(value);
    }

    public LiftCarDirection(final ByteQueue queue) throws BACnetErrorException {
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
