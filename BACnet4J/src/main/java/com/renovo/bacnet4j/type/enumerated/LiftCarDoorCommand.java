
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LiftCarDoorCommand extends Enumerated {
    public static final LiftCarDoorCommand none = new LiftCarDoorCommand(0);
    public static final LiftCarDoorCommand open = new LiftCarDoorCommand(1);
    public static final LiftCarDoorCommand close = new LiftCarDoorCommand(2);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LiftCarDoorCommand forId(final int id) {
        LiftCarDoorCommand e = (LiftCarDoorCommand) idMap.get(id);
        if (e == null)
            e = new LiftCarDoorCommand(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LiftCarDoorCommand forName(final String name) {
        return (LiftCarDoorCommand) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LiftCarDoorCommand(final int value) {
        super(value);
    }

    public LiftCarDoorCommand(final ByteQueue queue) throws BACnetErrorException {
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
