
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class NetworkPortCommand extends Enumerated {
    public static final NetworkPortCommand idle = new NetworkPortCommand(0);
    public static final NetworkPortCommand discardChanges = new NetworkPortCommand(1);
    public static final NetworkPortCommand renewFdRegistration = new NetworkPortCommand(2);
    public static final NetworkPortCommand restartSlaveDiscovery = new NetworkPortCommand(3);
    public static final NetworkPortCommand renewDhcp = new NetworkPortCommand(4);
    public static final NetworkPortCommand restartAutorenegotiation = new NetworkPortCommand(5);
    public static final NetworkPortCommand disconnect = new NetworkPortCommand(6);
    public static final NetworkPortCommand restartPort = new NetworkPortCommand(7);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static NetworkPortCommand forId(final int id) {
        NetworkPortCommand e = (NetworkPortCommand) idMap.get(id);
        if (e == null)
            e = new NetworkPortCommand(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static NetworkPortCommand forName(final String name) {
        return (NetworkPortCommand) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private NetworkPortCommand(final int value) {
        super(value);
    }

    public NetworkPortCommand(final ByteQueue queue) throws BACnetErrorException {
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
