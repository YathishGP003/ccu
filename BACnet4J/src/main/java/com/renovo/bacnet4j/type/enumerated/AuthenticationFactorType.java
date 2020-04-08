
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AuthenticationFactorType extends Enumerated {
    public static final AuthenticationFactorType undefined = new AuthenticationFactorType(0);
    public static final AuthenticationFactorType error = new AuthenticationFactorType(1);
    public static final AuthenticationFactorType custom = new AuthenticationFactorType(2);
    public static final AuthenticationFactorType simpleNumber16 = new AuthenticationFactorType(3);
    public static final AuthenticationFactorType simpleNumber32 = new AuthenticationFactorType(4);
    public static final AuthenticationFactorType simpleNumber56 = new AuthenticationFactorType(5);
    public static final AuthenticationFactorType simpleAlphaNumeric = new AuthenticationFactorType(6);
    public static final AuthenticationFactorType abaTrack2 = new AuthenticationFactorType(7);
    public static final AuthenticationFactorType wiegand26 = new AuthenticationFactorType(8);
    public static final AuthenticationFactorType wiegand37 = new AuthenticationFactorType(9);
    public static final AuthenticationFactorType wiegand37Facility = new AuthenticationFactorType(10);
    public static final AuthenticationFactorType facility16Card32 = new AuthenticationFactorType(11);
    public static final AuthenticationFactorType facility32Card32 = new AuthenticationFactorType(12);
    public static final AuthenticationFactorType fascN = new AuthenticationFactorType(13);
    public static final AuthenticationFactorType fascNBcd = new AuthenticationFactorType(14);
    public static final AuthenticationFactorType fascNLarge = new AuthenticationFactorType(15);
    public static final AuthenticationFactorType fascNLargeBcd = new AuthenticationFactorType(16);
    public static final AuthenticationFactorType gsa75 = new AuthenticationFactorType(17);
    public static final AuthenticationFactorType chuid = new AuthenticationFactorType(18);
    public static final AuthenticationFactorType chuidFull = new AuthenticationFactorType(19);
    public static final AuthenticationFactorType guid = new AuthenticationFactorType(20);
    public static final AuthenticationFactorType cbeffA = new AuthenticationFactorType(21);
    public static final AuthenticationFactorType cbeffB = new AuthenticationFactorType(22);
    public static final AuthenticationFactorType cbeffC = new AuthenticationFactorType(23);
    public static final AuthenticationFactorType userPassword = new AuthenticationFactorType(24);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AuthenticationFactorType forId(final int id) {
        AuthenticationFactorType e = (AuthenticationFactorType) idMap.get(id);
        if (e == null)
            e = new AuthenticationFactorType(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AuthenticationFactorType forName(final String name) {
        return (AuthenticationFactorType) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AuthenticationFactorType(final int value) {
        super(value);
    }

    public AuthenticationFactorType(final ByteQueue queue) throws BACnetErrorException {
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
