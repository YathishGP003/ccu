
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class ErrorClass extends Enumerated {
    public static final ErrorClass device = new ErrorClass(0);
    public static final ErrorClass object = new ErrorClass(1);
    public static final ErrorClass property = new ErrorClass(2);
    public static final ErrorClass resources = new ErrorClass(3);
    public static final ErrorClass security = new ErrorClass(4);
    public static final ErrorClass services = new ErrorClass(5);
    public static final ErrorClass vt = new ErrorClass(6);
    public static final ErrorClass communication = new ErrorClass(7);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static ErrorClass forId(final int id) {
        ErrorClass e = (ErrorClass) idMap.get(id);
        if (e == null)
            e = new ErrorClass(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static ErrorClass forName(final String name) {
        return (ErrorClass) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private ErrorClass(final int value) {
        super(value);
    }

    public ErrorClass(final ByteQueue queue) throws BACnetErrorException {
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
