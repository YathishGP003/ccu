
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class ProgramState extends Enumerated {
    public static final ProgramState idle = new ProgramState(0);
    public static final ProgramState loading = new ProgramState(1);
    public static final ProgramState running = new ProgramState(2);
    public static final ProgramState waiting = new ProgramState(3);
    public static final ProgramState halted = new ProgramState(4);
    public static final ProgramState unloading = new ProgramState(5);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static ProgramState forId(final int id) {
        ProgramState e = (ProgramState) idMap.get(id);
        if (e == null)
            e = new ProgramState(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static ProgramState forName(final String name) {
        return (ProgramState) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private ProgramState(final int value) {
        super(value);
    }

    public ProgramState(final ByteQueue queue) throws BACnetErrorException {
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
