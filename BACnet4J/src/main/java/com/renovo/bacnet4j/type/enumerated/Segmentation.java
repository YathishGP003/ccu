
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class Segmentation extends Enumerated {
    public static final Segmentation segmentedBoth = new Segmentation(0);
    public static final Segmentation segmentedTransmit = new Segmentation(1);
    public static final Segmentation segmentedReceive = new Segmentation(2);
    public static final Segmentation noSegmentation = new Segmentation(3);

    public boolean hasTransmitSegmentation() {
        return this.equals(segmentedBoth) || this.equals(segmentedTransmit);
    }

    public boolean hasReceiveSegmentation() {
        return this.equals(segmentedBoth) || this.equals(segmentedReceive);
    }

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static Segmentation forId(final int id) {
        Segmentation e = (Segmentation) idMap.get(id);
        if (e == null)
            e = new Segmentation(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static Segmentation forName(final String name) {
        return (Segmentation) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private Segmentation(final int value) {
        super(value);
    }

    public Segmentation(final ByteQueue queue) throws BACnetErrorException {
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
