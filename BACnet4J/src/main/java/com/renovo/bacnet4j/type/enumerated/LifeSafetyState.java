
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class LifeSafetyState extends Enumerated {
    public static final LifeSafetyState quiet = new LifeSafetyState(0);
    public static final LifeSafetyState preAlarm = new LifeSafetyState(1);
    public static final LifeSafetyState alarm = new LifeSafetyState(2);
    public static final LifeSafetyState fault = new LifeSafetyState(3);
    public static final LifeSafetyState faultPreAlarm = new LifeSafetyState(4);
    public static final LifeSafetyState faultAlarm = new LifeSafetyState(5);
    public static final LifeSafetyState notReady = new LifeSafetyState(6);
    public static final LifeSafetyState active = new LifeSafetyState(7);
    public static final LifeSafetyState tamper = new LifeSafetyState(8);
    public static final LifeSafetyState testAlarm = new LifeSafetyState(9);
    public static final LifeSafetyState testActive = new LifeSafetyState(10);
    public static final LifeSafetyState testFault = new LifeSafetyState(11);
    public static final LifeSafetyState testFaultAlarm = new LifeSafetyState(12);
    public static final LifeSafetyState holdup = new LifeSafetyState(13);
    public static final LifeSafetyState duress = new LifeSafetyState(14);
    public static final LifeSafetyState tamperAlarm = new LifeSafetyState(15);
    public static final LifeSafetyState abnormal = new LifeSafetyState(16);
    public static final LifeSafetyState emergencyPower = new LifeSafetyState(17);
    public static final LifeSafetyState delayed = new LifeSafetyState(18);
    public static final LifeSafetyState blocked = new LifeSafetyState(19);
    public static final LifeSafetyState localAlarm = new LifeSafetyState(20);
    public static final LifeSafetyState generalAlarm = new LifeSafetyState(21);
    public static final LifeSafetyState supervisory = new LifeSafetyState(22);
    public static final LifeSafetyState testSupervisory = new LifeSafetyState(23);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static LifeSafetyState forId(final int id) {
        LifeSafetyState e = (LifeSafetyState) idMap.get(id);
        if (e == null)
            e = new LifeSafetyState(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static LifeSafetyState forName(final String name) {
        return (LifeSafetyState) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private LifeSafetyState(final int value) {
        super(value);
    }

    public LifeSafetyState(final ByteQueue queue) throws BACnetErrorException {
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
