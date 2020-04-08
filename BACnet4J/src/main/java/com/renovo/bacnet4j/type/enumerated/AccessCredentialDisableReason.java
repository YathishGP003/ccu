
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AccessCredentialDisableReason extends Enumerated {
    public static final AccessCredentialDisableReason disabled = new AccessCredentialDisableReason(0);
    public static final AccessCredentialDisableReason disabledNeedsProvisioning = new AccessCredentialDisableReason(1);
    public static final AccessCredentialDisableReason disabledUnassigned = new AccessCredentialDisableReason(2);
    public static final AccessCredentialDisableReason disabledNotYetActive = new AccessCredentialDisableReason(3);
    public static final AccessCredentialDisableReason disabledExpired = new AccessCredentialDisableReason(4);
    public static final AccessCredentialDisableReason disabledLockout = new AccessCredentialDisableReason(5);
    public static final AccessCredentialDisableReason disabledMaxDays = new AccessCredentialDisableReason(6);
    public static final AccessCredentialDisableReason disabledMaxUses = new AccessCredentialDisableReason(7);
    public static final AccessCredentialDisableReason disabledInactivity = new AccessCredentialDisableReason(8);
    public static final AccessCredentialDisableReason disabledManual = new AccessCredentialDisableReason(9);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AccessCredentialDisableReason forId(final int id) {
        AccessCredentialDisableReason e = (AccessCredentialDisableReason) idMap.get(id);
        if (e == null)
            e = new AccessCredentialDisableReason(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AccessCredentialDisableReason forName(final String name) {
        return (AccessCredentialDisableReason) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AccessCredentialDisableReason(final int value) {
        super(value);
    }

    public AccessCredentialDisableReason(final ByteQueue queue) throws BACnetErrorException {
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
