
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class FaultType extends Enumerated {
    public static final FaultType none = new FaultType(0);
    public static final FaultType faultCharacterString = new FaultType(1);
    public static final FaultType faultExtended = new FaultType(2);
    public static final FaultType faultLifeSafety = new FaultType(3);
    public static final FaultType faultState = new FaultType(4);
    public static final FaultType faultStatusFlags = new FaultType(5);
    public static final FaultType faultOutOfRange = new FaultType(6);
    public static final FaultType faultListed = new FaultType(7);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static FaultType forId(final int id) {
        FaultType e = (FaultType) idMap.get(id);
        if (e == null)
            e = new FaultType(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static FaultType forName(final String name) {
        return (FaultType) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private FaultType(final int value) {
        super(value);
    }

    public FaultType(final ByteQueue queue) throws BACnetErrorException {
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
