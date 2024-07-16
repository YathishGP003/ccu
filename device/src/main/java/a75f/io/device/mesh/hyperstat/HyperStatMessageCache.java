package a75f.io.device.mesh.hyperstat;


import java.util.HashMap;
import java.util.Objects;

import a75f.io.device.HyperStat;
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
    private HashMap<Integer, HyperStat.HyperStatControlsMessage_t> hyperstatMessage;
    
    private HyperStatMessageCache(){
        messages = new HashMap<>();
        hyperstatMessage = new HashMap<>();
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
     * We need to send a message to hyperstat only if this method returns false.
     *
     * @param hyperStatAddress
     * @param simpleName
     * @param messageHash
     * @return true if the message is already existing in the cache
     */
    public boolean checkAndInsert(int hyperStatAddress, String simpleName, Integer messageHash) {

        if (messages.containsKey(hyperStatAddress)) {
            HashMap<String, Integer> stringIntegerHashMap = messages.get(hyperStatAddress);
            if (stringIntegerHashMap.containsKey(simpleName) &&
                            messageHash.equals(stringIntegerHashMap.get(simpleName))) {
               return true;
            }
        }
        else {
            messages.put(hyperStatAddress, new HashMap<>());
        }
        messages.get(hyperStatAddress).put(simpleName, messageHash);
        return false;
    }

    /**
     *
     * Check if the message exists in cache for a particular node.
     * If the node does not exist , create a new Map for the node.
     * Else update node message map with the new message.
     * We need to send a message to hyperstat only if this method returns false.
     *
     * @param hyperStatAddress
     * @param newControlMessage
     * @return true if the message is already existing in the cache
     */
    public boolean checkControlMessage(int hyperStatAddress,
                                  HyperStat.HyperStatControlsMessage_t newControlMessage) {

        if (messages.containsKey(hyperStatAddress) && hyperstatMessage.containsKey(hyperStatAddress)) {
            if (!compareControlMessage(Objects.requireNonNull(hyperstatMessage.get(hyperStatAddress)),newControlMessage)) {
                CcuLog.d(L.TAG_CCU_SERIAL,"Messages are same as previous or analogout is less than 5%");
                return true;
            }
            CcuLog.d(L.TAG_CCU_SERIAL,"Messages are not same as previous or analogout is greater than 5%");
        }
        else {
            messages.put(hyperStatAddress, new HashMap<>());
            hyperstatMessage.put(hyperStatAddress,newControlMessage);
            return false;
        }

        hyperstatMessage.put(hyperStatAddress,newControlMessage);
        return false;
    }

    private boolean compareControlMessage(HyperStat.HyperStatControlsMessage_t oldControlMessage,
                                          HyperStat.HyperStatControlsMessage_t newControlMessage) {
        return  (oldControlMessage.getRelay1() != newControlMessage.getRelay1()
                || (oldControlMessage.getRelay2() != newControlMessage.getRelay2())
                || (oldControlMessage.getRelay3() != newControlMessage.getRelay3())
                || (oldControlMessage.getRelay4() != newControlMessage.getRelay4())
                || (oldControlMessage.getRelay5() != newControlMessage.getRelay5())
                || (oldControlMessage.getRelay6() != newControlMessage.getRelay6())
                || (oldControlMessage.getConditioningMode() != newControlMessage.getConditioningMode())
                || (oldControlMessage.getConditioningModeValue() != newControlMessage.getConditioningModeValue())
                || (oldControlMessage.getFanSpeed() != newControlMessage.getFanSpeed())
                || (oldControlMessage.getFanSpeedValue() != newControlMessage.getFanSpeedValue())
                || (oldControlMessage.getOperatingMode() != newControlMessage.getOperatingMode())
                || (oldControlMessage.getOperatingModeValue() != newControlMessage.getOperatingModeValue())
                || (oldControlMessage.getSetTempCooling() != newControlMessage.getSetTempCooling())
                || (oldControlMessage.getSetTempHeating() != newControlMessage.getSetTempHeating())
                || (oldControlMessage.getReset() != newControlMessage.getReset())
                || (calculatePercentage(oldControlMessage.getAnalogOut1().getPercent(),
                newControlMessage.getAnalogOut1().getPercent()) >= 5)
                || (calculatePercentage(oldControlMessage.getAnalogOut2().getPercent(),
                newControlMessage.getAnalogOut2().getPercent()) >= 5)
                || (calculatePercentage(oldControlMessage.getAnalogOut3().getPercent(),
                newControlMessage.getAnalogOut3().getPercent()) >= 5)) ;

    }

    private double calculatePercentage(int oldAnalogVal, int newAnalogVal) {
        CcuLog.d(L.TAG_CCU_SERIAL,"oldAnalogVal = "+oldAnalogVal+" nenewAnalogVal = "
                +newAnalogVal+"\n return ="+Math.abs((oldAnalogVal) - (newAnalogVal)));
        return Math.abs((oldAnalogVal) - (newAnalogVal));
    }


}
