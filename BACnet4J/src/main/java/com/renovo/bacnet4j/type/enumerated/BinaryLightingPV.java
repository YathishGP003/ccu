
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class BinaryLightingPV extends Enumerated {
    public static final BinaryLightingPV off = new BinaryLightingPV(0);
    public static final BinaryLightingPV on = new BinaryLightingPV(1);
    public static final BinaryLightingPV warn = new BinaryLightingPV(2);
    public static final BinaryLightingPV warnOff = new BinaryLightingPV(3);
    public static final BinaryLightingPV warnRelinquish = new BinaryLightingPV(4);
    public static final BinaryLightingPV stop = new BinaryLightingPV(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static BinaryLightingPV forId(final int id) {
        BinaryLightingPV e = (BinaryLightingPV) idMap.get(id);
        if (e == null)
            e = new BinaryLightingPV(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static BinaryLightingPV forName(final String name) {
        return (BinaryLightingPV) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private BinaryLightingPV(final int value) {
        super(value);
    }

    public BinaryLightingPV(final ByteQueue queue) throws BACnetErrorException {
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
