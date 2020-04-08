
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AccessPassbackMode extends Enumerated {
    public static final AccessPassbackMode passbackOff = new AccessPassbackMode(0);
    public static final AccessPassbackMode hardPassback = new AccessPassbackMode(1);
    public static final AccessPassbackMode softPassback = new AccessPassbackMode(2);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AccessPassbackMode forId(final int id) {
        AccessPassbackMode e = (AccessPassbackMode) idMap.get(id);
        if (e == null)
            e = new AccessPassbackMode(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AccessPassbackMode forName(final String name) {
        return (AccessPassbackMode) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AccessPassbackMode(final int value) {
        super(value);
    }

    public AccessPassbackMode(final ByteQueue queue) throws BACnetErrorException {
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
