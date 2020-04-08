
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class NetworkNumberQuality extends Enumerated {
    public static final NetworkNumberQuality unknown = new NetworkNumberQuality(0);
    public static final NetworkNumberQuality learned = new NetworkNumberQuality(1);
    public static final NetworkNumberQuality learnedConfigured = new NetworkNumberQuality(2);
    public static final NetworkNumberQuality configured = new NetworkNumberQuality(3);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static NetworkNumberQuality forId(final int id) {
        NetworkNumberQuality e = (NetworkNumberQuality) idMap.get(id);
        if (e == null)
            e = new NetworkNumberQuality(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static NetworkNumberQuality forName(final String name) {
        return (NetworkNumberQuality) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private NetworkNumberQuality(final int value) {
        super(value);
    }

    public NetworkNumberQuality(final ByteQueue queue) throws BACnetErrorException {
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
