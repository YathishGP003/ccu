
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AuthorizationExemption extends Enumerated {
    public static final AuthorizationExemption passback = new AuthorizationExemption(0);
    public static final AuthorizationExemption occupancyCheck = new AuthorizationExemption(1);
    public static final AuthorizationExemption accessRights = new AuthorizationExemption(2);
    public static final AuthorizationExemption lockout = new AuthorizationExemption(3);
    public static final AuthorizationExemption deny = new AuthorizationExemption(4);
    public static final AuthorizationExemption verification = new AuthorizationExemption(5);
    public static final AuthorizationExemption authorizationDelay = new AuthorizationExemption(6);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AuthorizationExemption forId(final int id) {
        AuthorizationExemption e = (AuthorizationExemption) idMap.get(id);
        if (e == null)
            e = new AuthorizationExemption(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AuthorizationExemption forName(final String name) {
        return (AuthorizationExemption) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AuthorizationExemption(final int value) {
        super(value);
    }

    public AuthorizationExemption(final ByteQueue queue) throws BACnetErrorException {
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
