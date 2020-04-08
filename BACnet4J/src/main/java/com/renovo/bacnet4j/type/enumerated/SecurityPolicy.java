
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
public class SecurityPolicy extends Enumerated {
    public static final SecurityPolicy plainNonTrusted = new SecurityPolicy(0);
    public static final SecurityPolicy plainTrusted = new SecurityPolicy(1);
    public static final SecurityPolicy signedTrusted = new SecurityPolicy(2);
    public static final SecurityPolicy encryptedTrusted = new SecurityPolicy(3);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static SecurityPolicy forId(final int id) {
        SecurityPolicy e = (SecurityPolicy) idMap.get(id);
        if (e == null)
            e = new SecurityPolicy(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static SecurityPolicy forName(final String name) {
        return (SecurityPolicy) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private SecurityPolicy(final int value) {
        super(value);
    }

    public SecurityPolicy(final ByteQueue queue) throws BACnetErrorException {
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
