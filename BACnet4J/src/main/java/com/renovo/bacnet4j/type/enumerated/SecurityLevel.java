
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class SecurityLevel extends Enumerated {
    public static final SecurityLevel incapable = new SecurityLevel(0);
    public static final SecurityLevel plain = new SecurityLevel(1);
    public static final SecurityLevel signed = new SecurityLevel(2);
    public static final SecurityLevel encrypted = new SecurityLevel(3);
    public static final SecurityLevel signedEndToEnd = new SecurityLevel(4);
    public static final SecurityLevel encryptedEndToEnd = new SecurityLevel(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static SecurityLevel forId(final int id) {
        SecurityLevel e = (SecurityLevel) idMap.get(id);
        if (e == null)
            e = new SecurityLevel(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static SecurityLevel forName(final String name) {
        return (SecurityLevel) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private SecurityLevel(final int value) {
        super(value);
    }

    public SecurityLevel(final ByteQueue queue) throws BACnetErrorException {
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
