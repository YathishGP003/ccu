package a75f.io.device.mesh.mystat

import a75f.io.device.MyStat.MyStatControlsMessage_t
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import kotlin.math.abs

/**
 * Created by Manjunath K on 13-01-2025.
 */

object MyStatMsgCache {

    private val messages = HashMap<Int, HashMap<String, Int>>()
    private val mystatControlMessages = HashMap<Int, MyStatControlsMessage_t>()

    fun checkAndInsert(nodeAddress: Int, simpleName: String, messageHash: Int): Boolean {
        if (messages.containsKey(nodeAddress)) {
            val stringIntegerHashMap = messages[nodeAddress]!!
            if (stringIntegerHashMap.containsKey(simpleName) && messageHash == stringIntegerHashMap[simpleName]) {
                return true
            }
        } else {
            messages[nodeAddress] = HashMap()
        }
        messages[nodeAddress]!![simpleName] = messageHash
        return false
    }


    fun checkControlMessage(
        nodeAddress: Int,
        newControlMessage: MyStatControlsMessage_t
    ): Boolean {
        if (messages.containsKey(nodeAddress) && mystatControlMessages.containsKey(nodeAddress)) {
            if (!compareControlMessage(mystatControlMessages[nodeAddress]!!, newControlMessage)
            ) {
                CcuLog.d(
                    L.TAG_CCU_SERIAL,
                    "Messages are same as previous or analogout is less than 5%"
                )
                return true
            }
            CcuLog.d(
                L.TAG_CCU_SERIAL,
                "Messages are not same as previous or analogout is greater than 5%"
            )
        } else {
            messages[nodeAddress] = HashMap()
            mystatControlMessages[nodeAddress] = newControlMessage
            return false
        }

        mystatControlMessages[nodeAddress] = newControlMessage
        return false
    }

    private fun compareControlMessage(
        oldControlMessage: MyStatControlsMessage_t,
        newControlMessage: MyStatControlsMessage_t
    ): Boolean {
        return ((oldControlMessage.relayBitmap != newControlMessage.relayBitmap)
                || (oldControlMessage.conditioningMode != newControlMessage.conditioningMode)
                || (oldControlMessage.conditioningModeValue != newControlMessage.conditioningModeValue)
                || (oldControlMessage.fanSpeed != newControlMessage.fanSpeed)
                || (oldControlMessage.fanSpeedValue != newControlMessage.fanSpeedValue)
                || (oldControlMessage.operatingMode != newControlMessage.operatingMode)
                || (oldControlMessage.operatingModeValue != newControlMessage.operatingModeValue)
                || (oldControlMessage.setTempCooling != newControlMessage.setTempCooling)
                || (oldControlMessage.setTempHeating != newControlMessage.setTempHeating)
                || (oldControlMessage.reset != newControlMessage.reset)
                || (oldControlMessage.unoccupiedMode != newControlMessage.unoccupiedMode)
                || (calculatePercentage(
            oldControlMessage.analogOut1.percent,
            newControlMessage.analogOut1.percent
        ) >= 5))
    }

    private fun calculatePercentage(oldAnalogVal: Int, newAnalogVal: Int): Double {
        CcuLog.d(L.TAG_CCU_SERIAL, "oldAnalogVal = $oldAnalogVal nenewAnalogVal = $newAnalogVal return =${abs(((oldAnalogVal) - (newAnalogVal)).toDouble())}")
        return abs(((oldAnalogVal) - (newAnalogVal)).toDouble())
    }
}
