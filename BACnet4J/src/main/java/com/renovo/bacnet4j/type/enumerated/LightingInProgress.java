
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LightingInProgress extends Enumerated {
    public static final LightingInProgress idle = new LightingInProgress(0);
    public static final LightingInProgress fadeActive = new LightingInProgress(1);
    public static final LightingInProgress rampActive = new LightingInProgress(2);
    public static final LightingInProgress notControlled = new LightingInProgress(3);
    public static final LightingInProgress other = new LightingInProgress(4);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LightingInProgress forId(final int id) {
        LightingInProgress e = (LightingInProgress) idMap.get(id);
        if (e == null)
            e = new LightingInProgress(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LightingInProgress forName(final String name) {
        return (LightingInProgress) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LightingInProgress(final int value) {
        super(value);
    }

    public LightingInProgress(final ByteQueue queue) throws BACnetErrorException {
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
