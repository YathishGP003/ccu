
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LifeSafetyOperation extends Enumerated {
    public static final LifeSafetyOperation none = new LifeSafetyOperation(0);
    public static final LifeSafetyOperation silence = new LifeSafetyOperation(1);
    public static final LifeSafetyOperation silenceAudible = new LifeSafetyOperation(2);
    public static final LifeSafetyOperation silenceVisual = new LifeSafetyOperation(3);
    public static final LifeSafetyOperation reset = new LifeSafetyOperation(4);
    public static final LifeSafetyOperation resetAlarm = new LifeSafetyOperation(5);
    public static final LifeSafetyOperation resetFault = new LifeSafetyOperation(6);
    public static final LifeSafetyOperation unsilence = new LifeSafetyOperation(7);
    public static final LifeSafetyOperation unsilenceAudible = new LifeSafetyOperation(8);
    public static final LifeSafetyOperation unsilenceVisual = new LifeSafetyOperation(9);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LifeSafetyOperation forId(final int id) {
        LifeSafetyOperation e = (LifeSafetyOperation) idMap.get(id);
        if (e == null)
            e = new LifeSafetyOperation(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LifeSafetyOperation forName(final String name) {
        return (LifeSafetyOperation) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LifeSafetyOperation(final int value) {
        super(value);
    }

    public LifeSafetyOperation(final ByteQueue queue) throws BACnetErrorException {
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
