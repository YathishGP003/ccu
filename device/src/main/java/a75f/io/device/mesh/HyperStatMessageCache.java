package a75f.io.device.mesh;

import java.util.HashMap;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class HyperStatMessageCache {
    
    private static HyperStatMessageCache instance;
    
    /**
     * Node 1001 -
     * 	"HyperStatSettingsMessage_t", 12345
     * 	"HyperStatControlsMessage_t", 12333
     * where 12345 & 12333 are Arrays.hashCode(byte[])
     */
    private HashMap<Integer, HashMap<String, Integer>> messages;
    
    private HyperStatMessageCache(){
        messages = new HashMap<>();
    }
    
    public static HyperStatMessageCache getInstance() {
        if (instance == null) {
            instance = new HyperStatMessageCache();
        }
        return instance;
    }
    
    /**
     *
     * Check if the message exists in cache for a particular node.
     * If the node does not exist , create a new Map for the node.
     * Else update node message map with the new message.
     *
     * We need to send a meesage to hyperstat only if this method return false.
     *
     * @param hyperStatAddress
     * @param simpleName
     * @param messageHash
     * @return true if the message is already existing in the cache
     */
    public boolean checkAndInsert(int hyperStatAddress, String simpleName, Integer messageHash) {
        if (messages.containsKey(hyperStatAddress)) {
            HashMap<String, Integer> stringIntegerHashMap = messages.get(hyperStatAddress);
            if (stringIntegerHashMap.containsKey(simpleName)) {
                Integer previousHash = stringIntegerHashMap.get(simpleName);
                if (previousHash.equals(messageHash)) {
                    CcuLog.d(L.TAG_CCU_SERIAL,"Message was already sent");
                    return true;
                }
            }
        }
        else {
            messages.put(hyperStatAddress, new HashMap<>());
        }
        messages.get(hyperStatAddress).put(simpleName, messageHash);
        return false;
    }
    
    
    
    
    
}
