
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class ProgramError extends Enumerated {
    public static final ProgramError normal = new ProgramError(0);
    public static final ProgramError loadFailed = new ProgramError(1);
    public static final ProgramError internal = new ProgramError(2);
    public static final ProgramError program = new ProgramError(3);
    public static final ProgramError other = new ProgramError(4);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static ProgramError forId(final int id) {
        ProgramError e = (ProgramError) idMap.get(id);
        if (e == null)
            e = new ProgramError(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static ProgramError forName(final String name) {
        return (ProgramError) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private ProgramError(final int value) {
        super(value);
    }

    public ProgramError(final ByteQueue queue) throws BACnetErrorException {
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
