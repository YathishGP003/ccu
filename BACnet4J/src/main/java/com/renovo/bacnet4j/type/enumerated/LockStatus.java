
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

/**
 * @author Suresh Kumar
 */
public class LockStatus extends Enumerated {
    public static final LockStatus locked = new LockStatus(0);
    public static final LockStatus unlocked = new LockStatus(1);
    public static final LockStatus fault = new LockStatus(2);
    public static final LockStatus unused = new LockStatus(3);
    public static final LockStatus unknown = new LockStatus(4);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LockStatus forId(final int id) {
        LockStatus e = (LockStatus) idMap.get(id);
        if (e == null)
            e = new LockStatus(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LockStatus forName(final String name) {
        return (LockStatus) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LockStatus(final int value) {
        super(value);
    }

    public LockStatus(final ByteQueue queue) throws BACnetErrorException {
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
