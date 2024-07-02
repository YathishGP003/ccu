package a75f.io.device.mesh.hypersplit;

import java.util.HashMap;
import java.util.Objects;

import a75f.io.device.HyperSplit;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class HyperSplitMessageCache {
    
    private static HyperSplitMessageCache instance;

    /**
     * Node 1001 -
     * 	"HyperSplitSettingsMessage_t", 12345
     * 	"HyperSplitControlsMessage_t", 12333
     * where 12345 & 12333 are Arrays.hashCode(byte[])
     */
    private HashMap<Integer, HashMap<String, Integer>> messages;
    private HashMap<Integer, HyperSplit.HyperSplitControlsMessage_t> hypersplitMessage;

    private HyperSplitMessageCache(){
        messages = new HashMap<>();
        hypersplitMessage = new HashMap<>();
    }

    public static HyperSplitMessageCache getInstance() {
        if (instance == null) {
            instance = new HyperSplitMessageCache();
        }
        return instance;
    }

    /**
     *
     * Check if the message exists in cache for a particular node.
     * If the node does not exist , create a new Map for the node.
     * Else update node message map with the new message.
     * We need to send a message to hypersplit only if this method returns false.
     * @param hyperSplitAddress
     * @param simpleName
     * @param messageHash
     * @return true if the message is already existing in the cache
     */
    public boolean checkAndInsert(int hyperSplitAddress, String simpleName, Integer messageHash) {

        if (messages.containsKey(hyperSplitAddress)) {
            HashMap<String, Integer> stringIntegerHashMap = messages.get(hyperSplitAddress);
            if (stringIntegerHashMap.containsKey(simpleName) &&
                    messageHash.equals(stringIntegerHashMap.get(simpleName))) {
                return true;
            }
        }
        else {
            messages.put(hyperSplitAddress, new HashMap<>());
        }
        messages.get(hyperSplitAddress).put(simpleName, messageHash);
        return false;
    }

    /**
     *
     * Check if the message exists in cache for a particular node.
     * If the node does not exist , create a new Map for the node.
     * Else update node message map with the new message.
     * <p>
     * We need to send a message to hypersplit only if this method returns false.
     *
     * @param hyperSplitAddress
     * @param newControlMessage
     * @return true if the message is already existing in the cache
     */
    public boolean checkControlMessage(int hyperSplitAddress,
                                       HyperSplit.HyperSplitControlsMessage_t newControlMessage) {

        if (messages.containsKey(hyperSplitAddress) && hypersplitMessage.containsKey(hyperSplitAddress)) {
            if (!compareControlMessage(Objects.requireNonNull(hypersplitMessage.get(hyperSplitAddress)),newControlMessage)) {
                CcuLog.d(L.TAG_CCU_SERIAL,"Messages are same as previous or analogout is less than 5%");
                return true;
            }
            CcuLog.d(L.TAG_CCU_SERIAL,"Messages are not same as previous or analogout is greater than 5%");
        }
        else {
            messages.put(hyperSplitAddress, new HashMap<>());
            hypersplitMessage.put(hyperSplitAddress,newControlMessage);
            return false;
        }

        hypersplitMessage.put(hyperSplitAddress,newControlMessage);
        return false;
    }


    private boolean compareControlMessage(HyperSplit.HyperSplitControlsMessage_t oldControlMessage,
                                          HyperSplit.HyperSplitControlsMessage_t newControlMessage) {
        return  (oldControlMessage.getRelay1() != newControlMessage.getRelay1()
                || (oldControlMessage.getRelay2() != newControlMessage.getRelay2())
                || (oldControlMessage.getRelay3() != newControlMessage.getRelay3())
                || (oldControlMessage.getRelay4() != newControlMessage.getRelay4())
                || (oldControlMessage.getRelay5() != newControlMessage.getRelay5())
                || (oldControlMessage.getRelay6() != newControlMessage.getRelay6())
                || (oldControlMessage.getRelay7() != newControlMessage.getRelay7())
                || (oldControlMessage.getRelay8() != newControlMessage.getRelay8())
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
                newControlMessage.getAnalogOut3().getPercent()) >= 5)
                || (calculatePercentage(oldControlMessage.getAnalogOut4().getPercent(),
                newControlMessage.getAnalogOut4().getPercent()) >= 5));

    }

    private double calculatePercentage(int oldAnalogVal, int newAnalogVal) {
        CcuLog.d(L.TAG_CCU_SERIAL,"oldAnalogVal = "+oldAnalogVal+" nenewAnalogVal = "
                +newAnalogVal+"\n return ="+Math.abs((oldAnalogVal) - (newAnalogVal)));
        return Math.abs((oldAnalogVal) - (newAnalogVal));
    }

}
