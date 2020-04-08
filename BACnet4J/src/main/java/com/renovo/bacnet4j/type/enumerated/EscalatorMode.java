
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class EscalatorMode extends Enumerated {
    public static final EscalatorMode unknown = new EscalatorMode(0);
    public static final EscalatorMode stop = new EscalatorMode(1);
    public static final EscalatorMode up = new EscalatorMode(2);
    public static final EscalatorMode down = new EscalatorMode(3);
    public static final EscalatorMode inspection = new EscalatorMode(4);
    public static final EscalatorMode outOfService = new EscalatorMode(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static EscalatorMode forId(final int id) {
        EscalatorMode e = (EscalatorMode) idMap.get(id);
        if (e == null)
            e = new EscalatorMode(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static EscalatorMode forName(final String name) {
        return (EscalatorMode) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EscalatorMode(final int value) {
        super(value);
    }

    public EscalatorMode(final ByteQueue queue) throws BACnetErrorException {
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
