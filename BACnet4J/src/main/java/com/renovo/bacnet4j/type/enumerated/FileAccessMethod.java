
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class FileAccessMethod extends Enumerated {
    public static final FileAccessMethod recordAccess = new FileAccessMethod(0);
    public static final FileAccessMethod streamAccess = new FileAccessMethod(1);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static FileAccessMethod forId(final int id) {
        FileAccessMethod e = (FileAccessMethod) idMap.get(id);
        if (e == null)
            e = new FileAccessMethod(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static FileAccessMethod forName(final String name) {
        return (FileAccessMethod) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private FileAccessMethod(final int value) {
        super(value);
    }

    public FileAccessMethod(final ByteQueue queue) throws BACnetErrorException {
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
