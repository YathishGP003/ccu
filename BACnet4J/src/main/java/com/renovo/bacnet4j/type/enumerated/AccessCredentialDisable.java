
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AccessCredentialDisable extends Enumerated {
    public static final AccessCredentialDisable none = new AccessCredentialDisable(0);
    public static final AccessCredentialDisable disable = new AccessCredentialDisable(1);
    public static final AccessCredentialDisable disableManual = new AccessCredentialDisable(2);
    public static final AccessCredentialDisable disableLockout = new AccessCredentialDisable(3);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AccessCredentialDisable forId(final int id) {
        AccessCredentialDisable e = (AccessCredentialDisable) idMap.get(id);
        if (e == null)
            e = new AccessCredentialDisable(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AccessCredentialDisable forName(final String name) {
        return (AccessCredentialDisable) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AccessCredentialDisable(final int value) {
        super(value);
    }

    public AccessCredentialDisable(final ByteQueue queue) throws BACnetErrorException {
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
