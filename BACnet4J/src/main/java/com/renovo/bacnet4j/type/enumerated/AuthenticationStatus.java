
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class AuthenticationStatus extends Enumerated {
    public static final AuthenticationStatus notReady = new AuthenticationStatus(0);
    public static final AuthenticationStatus ready = new AuthenticationStatus(1);
    public static final AuthenticationStatus disabled = new AuthenticationStatus(2);
    public static final AuthenticationStatus waitingForAuthenticationFactor = new AuthenticationStatus(3);
    public static final AuthenticationStatus waitingForAccompaniment = new AuthenticationStatus(4);
    public static final AuthenticationStatus waitingForVerification = new AuthenticationStatus(5);
    public static final AuthenticationStatus inProgress = new AuthenticationStatus(6);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static AuthenticationStatus forId(final int id) {
        AuthenticationStatus e = (AuthenticationStatus) idMap.get(id);
        if (e == null)
            e = new AuthenticationStatus(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static AuthenticationStatus forName(final String name) {
        return (AuthenticationStatus) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private AuthenticationStatus(final int value) {
        super(value);
    }

    public AuthenticationStatus(final ByteQueue queue) throws BACnetErrorException {
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
