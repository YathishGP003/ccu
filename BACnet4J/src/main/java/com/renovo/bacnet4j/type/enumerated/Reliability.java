
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class Reliability extends Enumerated {
    public static final Reliability noFaultDetected = new Reliability(0);
    public static final Reliability noSensor = new Reliability(1);
    public static final Reliability overRange = new Reliability(2);
    public static final Reliability underRange = new Reliability(3);
    public static final Reliability openLoop = new Reliability(4);
    public static final Reliability shortedLoop = new Reliability(5);
    public static final Reliability noOutput = new Reliability(6);
    public static final Reliability unreliableOther = new Reliability(7);
    public static final Reliability processError = new Reliability(8);
    public static final Reliability multiStateFault = new Reliability(9);
    public static final Reliability configurationError = new Reliability(10);
    public static final Reliability communicationFailure = new Reliability(12);
    public static final Reliability memberFault = new Reliability(13);
    public static final Reliability monitoredObjectFault = new Reliability(14);
    public static final Reliability tripped = new Reliability(15);
    public static final Reliability lampFailure = new Reliability(16);
    public static final Reliability activationFailure = new Reliability(17);
    public static final Reliability renewDhcpFailure = new Reliability(18);
    public static final Reliability renewFdRgistrationFailure = new Reliability(19);
    public static final Reliability restartAutoNegotiationFailure = new Reliability(20);
    public static final Reliability restartFailure = new Reliability(21);
    public static final Reliability proprietaryCommandFailure = new Reliability(22);
    public static final Reliability faultsListed = new Reliability(23);
    public static final Reliability referencedObjectFault = new Reliability(24);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static Reliability forId(final int id) {
        Reliability e = (Reliability) idMap.get(id);
        if (e == null)
            e = new Reliability(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static Reliability forName(final String name) {
        return (Reliability) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private Reliability(final int value) {
        super(value);
    }

    public Reliability(final ByteQueue queue) throws BACnetErrorException {
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
