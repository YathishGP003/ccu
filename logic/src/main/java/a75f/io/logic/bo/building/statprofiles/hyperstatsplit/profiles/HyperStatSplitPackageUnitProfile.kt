package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles

/**
 * Created for HyperStat by Manjunath K on 22-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

abstract class HyperStatSplitPackageUnitProfile(equipRef: String, nodeAddress: Short): HyperStatSplitProfile(equipRef, nodeAddress) {

    var fanEnabledStatus = false
    /* Flags for checking if the current stage is lowest
    *  Eg: If FAN MEDIUM and FAN HIGH is used, then FAN MEDIUM is the lowest stage */
    var lowestStageFanLow = false
    var lowestStageFanMedium = false
    var lowestStageFanHigh = false

    fun resetFanStatus() {
        fanEnabledStatus = false
        lowestStageFanLow = false
        lowestStageFanMedium = false
        lowestStageFanHigh = false
    }

    /*
 fun setFanEnabledStatus(status: Boolean) {
        fanEnabledStatus = status
    }
    fun setFanLowestFanLowStatus(status: Boolean) {
        lowestStageFanLow = status
    }
    fun setFanLowestFanMediumStatus(status: Boolean) {
        lowestStageFanMedium = status
    }

    fun setFanLowestFanHighStatus(status: Boolean) {
        lowestStageFanHigh = status
    }

    fun doFanLowSpeed(
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        divider: Int,
        prePurgeRunning: Boolean
    ) {

        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > relayActivationHysteresis)
                relayState = 1.0
            else if (fanLoopOutput <= 0)
                relayState = 0.0
            else {
                val currentPortStatus: Double = hssEquip.fanLowSpeed.readHisVal()
                relayState = if (currentPortStatus > 0) 1.0 else 0.0
            }

            // For Title 24 compliance, set relay when one of the fan is mapped to enabled and loop > 0
            if (fanEnabledStatus && (fanLoopOutput > 0) && lowestStageFanLow)
                relayState = 1.0
            else if(prePurgeRunning)
                relayState = 1.0

        } else {
            relayState = 1.0
        }
        if (relayState != -1.0) {
            hssEquip.fanLowSpeed.writeHisVal(relayState)
            if (relayState == 1.0) {
                relayStages[Stage.FAN_1.displayName] = 1
            }
        }

    }

    fun doFanMediumSpeed(
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        divider: Int,
        relayStages: HashMap<String, Int>,
        prePurgeRunning: Boolean
    ) {

        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > (divider + (relayActivationHysteresis / 2)))
                relayState = 1.0
            else if (fanLoopOutput <= (divider - (relayActivationHysteresis / 2)))
                relayState = 0.0
            else {
                val currentPortStatus: Double = hssEquip.fanMediumSpeed.readHisVal()
                relayState = if (currentPortStatus > 0) 1.0 else 0.0
            }

            // For Title 24 compliance, check if fanEnabled is mapped and fanMedium is the lowest
            if (fanEnabledStatus && (fanLoopOutput > 0) && lowestStageFanMedium)
                relayState = 1.0
            else if(prePurgeRunning)
                relayState = 1.0

        } else {
            relayState = if (fanMode == StandaloneFanStage.MEDIUM_CUR_OCC
                || fanMode == StandaloneFanStage.MEDIUM_OCC
                || fanMode == StandaloneFanStage.MEDIUM_ALL_TIME
                || fanMode == StandaloneFanStage.HIGH_CUR_OCC
                || fanMode == StandaloneFanStage.HIGH_OCC
                || fanMode == StandaloneFanStage.HIGH_ALL_TIME
            ) 1.0 else 0.0
        }
        if (relayState != -1.0) {
            hssEquip.fanMediumSpeed.writeHisVal(relayState)
            CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "${hssEquip.fanMediumSpeed.id} = FanMediumSpeed:  $relayState")
            if (relayState == 1.0) {
                relayStages[Stage.FAN_2.displayName] = 1
            }
        }

    }

    fun doFanHighSpeed(
        fanMode: StandaloneFanStage,
        fanLoopOutput: Int,
        relayActivationHysteresis: Int,
        relayStages: HashMap<String, Int>,
        prePurgeRunning: Boolean
    ) {
        var relayState = -1.0
        if (fanMode == StandaloneFanStage.AUTO) {
            if (fanLoopOutput > (66 + (relayActivationHysteresis / 2)))
                relayState = 1.0
            else if (fanLoopOutput <= (66 - (relayActivationHysteresis / 2)))
                relayState = 0.0
            else {
                val currentPortStatus: Double = hssEquip.fanHighSpeed.readHisVal()
                relayState = if (currentPortStatus > 0) 1.0 else 0.0
            }

            // For Title 24 compliance, check if fanEnabled is mapped and fanHigh is the lowest
            if (fanEnabledStatus && (fanLoopOutput > 0) && lowestStageFanHigh)
                relayState = 1.0
            else if(prePurgeRunning)
                relayState = 1.0

        } else {
            relayState = if (fanMode == StandaloneFanStage.HIGH_CUR_OCC
                || fanMode == StandaloneFanStage.HIGH_OCC
                || fanMode == StandaloneFanStage.HIGH_ALL_TIME
            ) 1.0 else 0.0
        }
        if (relayState != -1.0) {
            hssEquip.fanHighSpeed.writeHisVal(relayState)
            CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "${hssEquip.fanHighSpeed.id} = FanHighSpeed:  $relayState")
            if (relayState == 1.0) {
                relayStages[Stage.FAN_3.displayName] = 1
            }
        }
    }
*/
}
