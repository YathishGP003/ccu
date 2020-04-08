
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LiftGroupMode extends Enumerated {
    public static final LiftGroupMode unknown = new LiftGroupMode(0);
    public static final LiftGroupMode normal = new LiftGroupMode(1);
    public static final LiftGroupMode downPeak = new LiftGroupMode(2);
    public static final LiftGroupMode twoWay = new LiftGroupMode(3);
    public static final LiftGroupMode fourWay = new LiftGroupMode(4);
    public static final LiftGroupMode emergencyPower = new LiftGroupMode(5);
    public static final LiftGroupMode upPeak = new LiftGroupMode(6);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LiftGroupMode forId(final int id) {
        LiftGroupMode e = (LiftGroupMode) idMap.get(id);
        if (e == null)
            e = new LiftGroupMode(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LiftGroupMode forName(final String name) {
        return (LiftGroupMode) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LiftGroupMode(final int value) {
        super(value);
    }

    public LiftGroupMode(final ByteQueue queue) throws BACnetErrorException {
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
