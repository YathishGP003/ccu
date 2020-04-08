
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AbortReason extends Enumerated {
    public static final AbortReason other = new AbortReason(0);
    public static final AbortReason bufferOverflow = new AbortReason(1);
    public static final AbortReason invalidApduInThisState = new AbortReason(2);
    public static final AbortReason preemptedByHigherPriorityTask = new AbortReason(3);
    public static final AbortReason segmentationNotSupported = new AbortReason(4);
    public static final AbortReason securityError = new AbortReason(5);
    public static final AbortReason insufficientSecurity = new AbortReason(6);
    public static final AbortReason windowSizeOutOfRange = new AbortReason(7);
    public static final AbortReason applicationExceededReplyTime = new AbortReason(8);
    public static final AbortReason outOfResources = new AbortReason(9);
    public static final AbortReason tsmTimeout = new AbortReason(10);
    public static final AbortReason apduTooLong = new AbortReason(11);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AbortReason forId(final int id) {
        AbortReason e = (AbortReason) idMap.get(id);
        if (e == null)
            e = new AbortReason(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AbortReason forName(final String name) {
        return (AbortReason) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AbortReason(final int value) {
        super(value);
    }

    public AbortReason(final ByteQueue queue) throws BACnetErrorException {
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
