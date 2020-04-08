
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.npdu.NPCI.NetworkPriority;
import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class MessagePriority extends Enumerated {
    public static final MessagePriority normal = new MessagePriority(0);
    public static final MessagePriority urgent = new MessagePriority(1);

    public NetworkPriority getNetworkPriority() {
        final int type = intValue();
        if (type == urgent.intValue())
            return NetworkPriority.urgent;
        return NetworkPriority.normal;
    }

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static MessagePriority forId(final int id) {
        MessagePriority e = (MessagePriority) idMap.get(id);
        if (e == null)
            e = new MessagePriority(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static MessagePriority forName(final String name) {
        return (MessagePriority) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private MessagePriority(final int value) {
        super(value);
    }

    public MessagePriority(final ByteQueue queue) throws BACnetErrorException {
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
