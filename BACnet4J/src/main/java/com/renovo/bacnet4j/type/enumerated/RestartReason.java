
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class RestartReason extends Enumerated {
    public static final RestartReason unknown = new RestartReason(0);
    public static final RestartReason coldstart = new RestartReason(1);
    public static final RestartReason warmstart = new RestartReason(2);
    public static final RestartReason detectedPowerLost = new RestartReason(3);
    public static final RestartReason detectedPoweredOff = new RestartReason(4);
    public static final RestartReason hardwareWatchdog = new RestartReason(5);
    public static final RestartReason softwareWatchdog = new RestartReason(6);
    public static final RestartReason suspended = new RestartReason(7);
    public static final RestartReason activateChanges = new RestartReason(8);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    static {
        Enumerated.init(RestartReason.class, idMap, nameMap, prettyMap);
    }

    public static RestartReason forId(final int id) {
        RestartReason e = (RestartReason) idMap.get(id);
        if (e == null)
            e = new RestartReason(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static RestartReason forName(final String name) {
        return (RestartReason) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private RestartReason(final int value) {
        super(value);
    }

    public RestartReason(final ByteQueue queue) throws BACnetErrorException {
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
