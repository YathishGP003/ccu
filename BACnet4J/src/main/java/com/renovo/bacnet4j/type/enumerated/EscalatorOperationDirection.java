
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class EscalatorOperationDirection extends Enumerated {
    public static final EscalatorOperationDirection unknown = new EscalatorOperationDirection(0);
    public static final EscalatorOperationDirection stopped = new EscalatorOperationDirection(1);
    public static final EscalatorOperationDirection upRatedSpeed = new EscalatorOperationDirection(2);
    public static final EscalatorOperationDirection upReducedSpeed = new EscalatorOperationDirection(3);
    public static final EscalatorOperationDirection downRatedSpeed = new EscalatorOperationDirection(4);
    public static final EscalatorOperationDirection downReducedSpeed = new EscalatorOperationDirection(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static EscalatorOperationDirection forId(final int id) {
        EscalatorOperationDirection e = (EscalatorOperationDirection) idMap.get(id);
        if (e == null)
            e = new EscalatorOperationDirection(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static EscalatorOperationDirection forName(final String name) {
        return (EscalatorOperationDirection) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EscalatorOperationDirection(final int value) {
        super(value);
    }

    public EscalatorOperationDirection(final ByteQueue queue) throws BACnetErrorException {
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
