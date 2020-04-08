
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class BinaryPV extends Enumerated {
    public static final BinaryPV inactive = new BinaryPV(0);
    public static final BinaryPV active = new BinaryPV(1);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        //Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        Enumerated.init(BinaryPV.class, idMap, nameMap, prettyMap);
    }

    public static BinaryPV forId(final int id) {
        BinaryPV e = (BinaryPV) idMap.get(id);
        if (e == null)
            e = new BinaryPV(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static BinaryPV forName(final String name) {
        return (BinaryPV) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private BinaryPV(final int value) {
        super(value);
    }

    public BinaryPV(final ByteQueue queue) throws BACnetErrorException {
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
