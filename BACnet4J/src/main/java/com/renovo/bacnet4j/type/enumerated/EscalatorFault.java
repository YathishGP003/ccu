
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class EscalatorFault extends Enumerated {
    public static final EscalatorFault controllerFault = new EscalatorFault(0);
    public static final EscalatorFault driveAndMotorFault = new EscalatorFault(1);
    public static final EscalatorFault mechanicalComponentFault = new EscalatorFault(2);
    public static final EscalatorFault overspeedFault = new EscalatorFault(3);
    public static final EscalatorFault powerSupplyFault = new EscalatorFault(4);
    public static final EscalatorFault safetyDeviceFault = new EscalatorFault(5);
    public static final EscalatorFault controllerSupplyFault = new EscalatorFault(6);
    public static final EscalatorFault driveTemperatureExceeded = new EscalatorFault(7);
    public static final EscalatorFault combPlateFault = new EscalatorFault(8);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static EscalatorFault forId(final int id) {
        EscalatorFault e = (EscalatorFault) idMap.get(id);
        if (e == null)
            e = new EscalatorFault(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static EscalatorFault forName(final String name) {
        return (EscalatorFault) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private EscalatorFault(final int value) {
        super(value);
    }

    public EscalatorFault(final ByteQueue queue) throws BACnetErrorException {
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
