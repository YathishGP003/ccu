
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class Relationship extends Enumerated {
    public static final Relationship unknown = new Relationship(0);
    public static final Relationship _default = new Relationship(1);
    public static final Relationship contains = new Relationship(2);
    public static final Relationship containedBy = new Relationship(3);
    public static final Relationship uses = new Relationship(4);
    public static final Relationship usedBy = new Relationship(5);
    public static final Relationship commands = new Relationship(6);
    public static final Relationship commandedBy = new Relationship(7);
    public static final Relationship adjusts = new Relationship(8);
    public static final Relationship adjustedBy = new Relationship(9);
    public static final Relationship ingress = new Relationship(10);
    public static final Relationship egress = new Relationship(11);
    public static final Relationship suppliesAir = new Relationship(12);
    public static final Relationship receivesAir = new Relationship(13);
    public static final Relationship suppliesHotAir = new Relationship(14);
    public static final Relationship receivesHotAir = new Relationship(15);
    public static final Relationship suppliesCoolAir = new Relationship(16);
    public static final Relationship receivesCoolAir = new Relationship(17);
    public static final Relationship suppliesPower = new Relationship(18);
    public static final Relationship receivesPower = new Relationship(19);
    public static final Relationship suppliesGas = new Relationship(20);
    public static final Relationship receivesGas = new Relationship(21);
    public static final Relationship suppliesWater = new Relationship(22);
    public static final Relationship receivesWater = new Relationship(23);
    public static final Relationship suppliesHotWater = new Relationship(24);
    public static final Relationship receivesHotWater = new Relationship(25);
    public static final Relationship suppliesCoolWater = new Relationship(26);
    public static final Relationship receivesCoolWater = new Relationship(27);
    public static final Relationship suppliesSteam = new Relationship(28);
    public static final Relationship receivesSteam = new Relationship(29);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static Relationship forId(final int id) {
        Relationship e = (Relationship) idMap.get(id);
        if (e == null)
            e = new Relationship(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static Relationship forName(final String name) {
        return (Relationship) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private Relationship(final int value) {
        super(value);
    }

    public Relationship(final ByteQueue queue) throws BACnetErrorException {
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
