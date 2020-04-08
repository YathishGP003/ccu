
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class DeviceStatus extends Enumerated {
    public static final DeviceStatus operational = new DeviceStatus(0);
    public static final DeviceStatus operationalReadOnly = new DeviceStatus(1);
    public static final DeviceStatus downloadRequired = new DeviceStatus(2);
    public static final DeviceStatus downloadInProgress = new DeviceStatus(3);
    public static final DeviceStatus nonOperational = new DeviceStatus(4);
    public static final DeviceStatus backupInProgress = new DeviceStatus(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static DeviceStatus forId(final int id) {
        DeviceStatus e = (DeviceStatus) idMap.get(id);
        if (e == null)
            e = new DeviceStatus(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static DeviceStatus forName(final String name) {
        return (DeviceStatus) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private DeviceStatus(final int value) {
        super(value);
    }

    public DeviceStatus(final ByteQueue queue) throws BACnetErrorException {
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
