
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LiftCarMode extends Enumerated {
    public static final LiftCarMode unknown = new LiftCarMode(0);
    public static final LiftCarMode normal = new LiftCarMode(1);
    public static final LiftCarMode vip = new LiftCarMode(2);
    public static final LiftCarMode homing = new LiftCarMode(3);
    public static final LiftCarMode parking = new LiftCarMode(4);
    public static final LiftCarMode attendantControl = new LiftCarMode(5);
    public static final LiftCarMode firefighterControl = new LiftCarMode(6);
    public static final LiftCarMode emergencyPower = new LiftCarMode(7);
    public static final LiftCarMode inspection = new LiftCarMode(8);
    public static final LiftCarMode cabinetRecall = new LiftCarMode(9);
    public static final LiftCarMode earthquakeOperation = new LiftCarMode(10);
    public static final LiftCarMode fireOperation = new LiftCarMode(11);
    public static final LiftCarMode outOfService = new LiftCarMode(12);
    public static final LiftCarMode occupantEvacuation = new LiftCarMode(13);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LiftCarMode forId(final int id) {
        LiftCarMode e = (LiftCarMode) idMap.get(id);
        if (e == null)
            e = new LiftCarMode(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LiftCarMode forName(final String name) {
        return (LiftCarMode) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LiftCarMode(final int value) {
        super(value);
    }

    public LiftCarMode(final ByteQueue queue) throws BACnetErrorException {
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
