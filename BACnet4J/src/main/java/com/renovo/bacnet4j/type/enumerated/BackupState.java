
package com.renovo.bacnet4j.type.enumerated;

import com.renovo.bacnet4j.exception.BACnetErrorException;

import java.util.HashMap;
import java.util.Map;

import com.renovo.bacnet4j.type.primitive.Enumerated;
import com.renovo.bacnet4j.util.sero.ByteQueue;
import java.util.Collections;

public class BackupState extends Enumerated {
    public static final BackupState idle = new BackupState(0);
    public static final BackupState preparingForBackup = new BackupState(1);
    public static final BackupState preparingForRestore = new BackupState(2);
    public static final BackupState performingABackup = new BackupState(3);
    public static final BackupState performingARestore = new BackupState(4);
    public static final BackupState backupFailure = new BackupState(5);
    public static final BackupState restoreFailure = new BackupState(6);

    private static final Map<Integer, Enumerated> idMap = new HashMap<>();
    private static final Map<String, Enumerated> nameMap = new HashMap<>();
    private static final Map<Integer, String> prettyMap = new HashMap<>();

    /*static {
        Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
    }*/

    public static BackupState forId(final int id) {
        BackupState e = (BackupState) idMap.get(id);
        if (e == null)
            e = new BackupState(id);
        return e;
    }

    public static String nameForId(final int id) {
        return prettyMap.get(id);
    }

    public static BackupState forName(final String name) {
        return (BackupState) Enumerated.forName(nameMap, name);
    }

    public static int size() {
        return idMap.size();
    }

    private BackupState(final int value) {
        super(value);
    }

    public BackupState(final ByteQueue queue) throws BACnetErrorException {
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
