
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LiftFault extends Enumerated {
    public static final LiftFault controllerFault = new LiftFault(0);
    public static final LiftFault driveAndMotorFault = new LiftFault(1);
    public static final LiftFault governorAndSafetyGearFault = new LiftFault(2);
    public static final LiftFault liftShaftDeviceFault = new LiftFault(3);
    public static final LiftFault powerSupplyFault = new LiftFault(4);
    public static final LiftFault safetyInterlockFault = new LiftFault(5);
    public static final LiftFault doorClosingFault = new LiftFault(6);
    public static final LiftFault doorOpeningFault = new LiftFault(7);
    public static final LiftFault carStoppedOutsideLandingZone = new LiftFault(8);
    public static final LiftFault callButtonStuck = new LiftFault(9);
    public static final LiftFault startFailure = new LiftFault(10);
    public static final LiftFault controllerSupplyFault = new LiftFault(11);
    public static final LiftFault selfTestFailure = new LiftFault(12);
    public static final LiftFault runtimeLimitExceeded = new LiftFault(13);
    public static final LiftFault positionLost = new LiftFault(14);
    public static final LiftFault driveTemperatureExceeded = new LiftFault(15);
    public static final LiftFault loadMeasurementFault = new LiftFault(16);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LiftFault forId(final int id) {
        LiftFault e = (LiftFault) idMap.get(id);
        if (e == null)
            e = new LiftFault(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LiftFault forName(final String name) {
        return (LiftFault) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LiftFault(final int value) {
        super(value);
    }

    public LiftFault(final ByteQueue queue) throws BACnetErrorException {
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
