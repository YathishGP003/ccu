
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AccessAuthenticationFactorDisable extends Enumerated {
    public static final AccessAuthenticationFactorDisable none = new AccessAuthenticationFactorDisable(0);
    public static final AccessAuthenticationFactorDisable disabled = new AccessAuthenticationFactorDisable(1);
    public static final AccessAuthenticationFactorDisable disabledLost = new AccessAuthenticationFactorDisable(2);
    public static final AccessAuthenticationFactorDisable disabledStolen = new AccessAuthenticationFactorDisable(3);
    public static final AccessAuthenticationFactorDisable disabledDamaged = new AccessAuthenticationFactorDisable(4);
    public static final AccessAuthenticationFactorDisable disabledDestroyed = new AccessAuthenticationFactorDisable(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AccessAuthenticationFactorDisable forId(final int id) {
        AccessAuthenticationFactorDisable e = (AccessAuthenticationFactorDisable) idMap.get(id);
        if (e == null)
            e = new AccessAuthenticationFactorDisable(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AccessAuthenticationFactorDisable forName(final String name) {
        return (AccessAuthenticationFactorDisable) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AccessAuthenticationFactorDisable(final int value) {
        super(value);
    }

    public AccessAuthenticationFactorDisable(final ByteQueue queue) throws BACnetErrorException {
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
