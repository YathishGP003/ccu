
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LiftCarDriveStatus extends Enumerated {
    public static final LiftCarDriveStatus unknown = new LiftCarDriveStatus(0);
    public static final LiftCarDriveStatus stationary = new LiftCarDriveStatus(1);
    public static final LiftCarDriveStatus braking = new LiftCarDriveStatus(2);
    public static final LiftCarDriveStatus accelerate = new LiftCarDriveStatus(3);
    public static final LiftCarDriveStatus decelerate = new LiftCarDriveStatus(4);
    public static final LiftCarDriveStatus ratedSpeed = new LiftCarDriveStatus(5);
    public static final LiftCarDriveStatus singleFloorJump = new LiftCarDriveStatus(6);
    public static final LiftCarDriveStatus twoFloorJump = new LiftCarDriveStatus(7);
    public static final LiftCarDriveStatus threeFloorJump = new LiftCarDriveStatus(8);
    public static final LiftCarDriveStatus multiFloorJump = new LiftCarDriveStatus(9);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LiftCarDriveStatus forId(final int id) {
        LiftCarDriveStatus e = (LiftCarDriveStatus) idMap.get(id);
        if (e == null)
            e = new LiftCarDriveStatus(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LiftCarDriveStatus forName(final String name) {
        return (LiftCarDriveStatus) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LiftCarDriveStatus(final int value) {
        super(value);
    }

    public LiftCarDriveStatus(final ByteQueue queue) throws BACnetErrorException {
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
