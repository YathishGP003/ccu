package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.AdvancedHybridSystemEquip
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.SystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.oao.OAOProfile
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.system.util.AhuSettings
import a75f.io.logic.bo.building.system.util.AhuTuners

/**
 * Common implementation of utility methods for VAV and DAB advanced AHUs
 */
class AdvancedAhuAlgoHandler (val equip: SystemEquip) {

    private fun getCoolingRelayState(stageIndex: Int, point: Point, systemCoolingLoop : Double,
                                     oaoProfile : OAOProfile?, relayHysteresis : Double, coolingStagesAvailable : Int): Boolean {
        val stageThreshold = if (oaoProfile != null && oaoProfile.isEconomizingAvailable) {
            100 * (stageIndex + 1) / (coolingStagesAvailable + 1)
        } else {
            100 * stageIndex / coolingStagesAvailable
        }
        val currentState = point.readHisVal()
        CcuLog.i(
            L.TAG_CCU_SYSTEM, "getCoolingRelayState: currentState: " +
                    "$currentState, stageThreshold: $stageThreshold, systemCoolingLoop: " +
                    "$systemCoolingLoop, relayHysteresis: $relayHysteresis")
        return if (currentState.toInt() == 0) systemCoolingLoop > (stageThreshold +.01) else
            systemCoolingLoop > (stageThreshold - relayHysteresis).coerceAtLeast(0.0)
    }

    private fun getHeatingRelayState(stageIndex: Int, point: Point, systemHeatingLoop: Double, relayHysteresis: Double, heatingStagesAvailable : Int) : Boolean {
        val stageThreshold = 100 * stageIndex / heatingStagesAvailable
        val currentState = point.readHisVal()
        CcuLog.i(
            L.TAG_CCU_SYSTEM, "getHeatingRelayState: currentState: " +
                    "$currentState, stageThreshold: $stageThreshold, systemHeatingLoop: " +
                    "$systemHeatingLoop, relayHysteresis: $relayHysteresis")
        return if (currentState.toInt() == 0) systemHeatingLoop > (stageThreshold +.01) else
            systemHeatingLoop > (stageThreshold - relayHysteresis).coerceAtLeast(0.0)
    }

    private fun getFanRelayState(stageIndex: Int, point: Point, systemFanLoop: Double,
                                 relayHysteresis: Double, fanStagesAvailable : Int,
                                 isSystemOccupied: Boolean, isStage1AllowToActive: Boolean) : Boolean{
        return when(stageIndex) {
            0 -> (isSystemOccupied || isStage1AllowToActive || isReheatActive(CCUHsApi.getInstance()))
            1 -> systemFanLoop > 0
            else -> {
                val stageThreshold = 100 * (stageIndex - 1) / (fanStagesAvailable -1)
                val currentState = point.readHisVal()
                CcuLog.i(
                    L.TAG_CCU_SYSTEM, "stageIndex $stageIndex getFanRelayState: currentState: " +
                            "$currentState, stageThreshold: $stageThreshold, systemFanLoop: " +
                            "$systemFanLoop, relayHysteresis: $relayHysteresis")
                if (currentState.toInt() == 0) systemFanLoop > (stageThreshold +.01) else
                    systemFanLoop > (stageThreshold - relayHysteresis).coerceAtLeast(0.0)
            }
        }
    }

    private fun getHumidifierRelayState(point: Point, humidity : Double, targetMin : Double, hysteresis : Double) : Boolean {
        val currentState = point.readHisVal()
        CcuLog.i(
            L.TAG_CCU_SYSTEM, "getHumidifierRelayState: currentState: " +
                    "$currentState, humidity: $humidity, targetMin: " +
                    "$targetMin, hysteresis: $hysteresis"
        )
        return if (humidity > 0 && humidity < targetMin) {
            true
        } else if (humidity > (targetMin + hysteresis)) {
            false
        } else {
            currentState > 0
        }
    }
    private fun getDehumidifierRelayState(point: Point, humidity : Double, targetMax : Double, hysteresis : Double) : Boolean {
        val currentState = point.readHisVal()
        CcuLog.i(
            L.TAG_CCU_SYSTEM, "getDehumidifierRelayState: currentState: " +
                    "$currentState, humidity: $humidity, targetMax: " +
                    "$targetMax, hysteresis: $hysteresis"
        )
        return if (humidity > targetMax) {
            true
        } else if (humidity < (targetMax - hysteresis)) {
            false
        } else {
            currentState > 0
        }
    }

    private fun getFanEnableRelayState(loadBasedFanLoop: Double, pressureFanLoop: Double) : Boolean{
        return ScheduleManager.getInstance().systemOccupancy == Occupancy.OCCUPIED || loadBasedFanLoop > 0 || pressureFanLoop > 0
    }

    private fun getOccupiedEnableRelayState() = ScheduleManager.getInstance().systemOccupancy == Occupancy.OCCUPIED

    private fun getAhuFreshAirFanRunCommandRelayState(isSystemOccupied: Boolean, systemCo2Loop : Double) : Boolean{
        return isSystemOccupied && systemCo2Loop > 0
    }

    private fun getExhaustFan1CommandRelayState(ahuSettings: AhuSettings, stageIndex: Int) : Boolean {
        return if(stageIndex == 0) {
            ahuSettings.connectEquip1.exhaustFanStage1.readHisVal() > 0
        } else {
            ahuSettings.connectEquip1.exhaustFanStage2.readHisVal() > 0
        }
    }

    fun getAdvancedAhuRelayState(
            association: Point, coolingStages: Int,
            heatingStages: Int, fanStages: Int, systemOccupied: Boolean,
            isConnectEquip: Boolean, ahuSettings: AhuSettings,
            ahuTuners: AhuTuners, isStage1AllowToActive: Boolean
    ): Pair<Point, Boolean> {

        val associatedPointName: String
        //Get logical point from association index
        val associatedPoint = if (isConnectEquip) {
            associatedPointName = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
            getDomainPointForName(associatedPointName, ahuSettings.connectEquip1)
        } else {
            associatedPointName = relayAssociationToDomainName(association.readDefaultVal().toInt())
            getDomainPointForName(associatedPointName, ahuSettings.systemEquip)

        }
        val stageIndex = getStageIndex(associatedPoint)
        CcuLog.i(L.TAG_CCU_SYSTEM, "getRelayState: associatedPoint: $associatedPointName, stage: $stageIndex ")
        val pointVal =  when (relayAssociationDomainNameToType(associatedPointName)) {
            AdvancedAhuRelayAssociationType.LOAD_COOLING -> if (!ahuSettings.isMechanicalCoolingAvailable) {
                getCoolingRelayState(
                    stageIndex, associatedPoint, ahuSettings.systemEquip.coolingLoopOutput.readHisVal(),
                    L.ccu().oaoProfile, ahuTuners.relayAActivationHysteresis, coolingStages
                )
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalCooling Not available")
                false
            }

            AdvancedAhuRelayAssociationType.LOAD_HEATING -> if (!ahuSettings.isMechanicalHeatingAvailable) {
                getHeatingRelayState(
                    stageIndex, associatedPoint, ahuSettings.systemEquip.heatingLoopOutput.readHisVal(),
                    ahuTuners.relayDeactivationHysteresis, heatingStages
                )
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalHeating Not available")
                false
            }

            AdvancedAhuRelayAssociationType.LOAD_FAN -> getFanRelayState(stageIndex, associatedPoint,
                    ahuSettings.systemEquip.fanLoopOutput.readHisVal(),
                    ahuTuners.relayDeactivationHysteresis, fanStages, systemOccupied, isStage1AllowToActive)

            AdvancedAhuRelayAssociationType.HUMIDIFIER -> {
                if (systemOccupied && ahuSettings.systemEquip.conditioningMode.readPriorityVal() > 0) {
                    getHumidifierRelayState(associatedPoint, ahuSettings.systemEquip.averageHumidity.readHisVal(),
                            ahuSettings.systemEquip.systemtargetMinInsideHumidity.readPriorityVal(), ahuTuners.humidityHysteresis)
                } else {
                    false
                }
            }

            AdvancedAhuRelayAssociationType.DEHUMIDIFIER ->
                if (systemOccupied && ahuSettings.systemEquip.conditioningMode.readPriorityVal() > 0) {
                    getDehumidifierRelayState(associatedPoint, ahuSettings.systemEquip.averageHumidity.readHisVal(),
                    ahuSettings.systemEquip.systemtargetMaxInsideHumidity.readPriorityVal(), ahuTuners.humidityHysteresis)
                } else {
                    false
                }

            AdvancedAhuRelayAssociationType.SAT_COOLING -> if (!ahuSettings.isMechanicalCoolingAvailable) {
                getCoolingRelayState(
                    stageIndex, associatedPoint, ahuSettings.systemEquip.satCoolingLoopOutput.readHisVal(),
                    L.ccu().oaoProfile, ahuTuners.relayDeactivationHysteresis, coolingStages
                )
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalCooling Not available")
                false
            }

            AdvancedAhuRelayAssociationType.SAT_HEATING -> if (!ahuSettings.isMechanicalHeatingAvailable) {
                getHeatingRelayState(stageIndex, associatedPoint, ahuSettings.systemEquip.satHeatingLoopOutput.readHisVal(),
                        ahuTuners.relayDeactivationHysteresis, heatingStages)
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalHeating Not available")
                false
            }

            AdvancedAhuRelayAssociationType.FAN_PRESSURE -> getFanRelayState(
                    stageIndex, associatedPoint, ahuSettings.systemEquip.fanPressureLoopOutput.readHisVal(),
                    ahuTuners.relayDeactivationHysteresis, fanStages, systemOccupied, isStage1AllowToActive)

            AdvancedAhuRelayAssociationType.FAN_ENABLE -> getFanEnableRelayState(
                    ahuSettings.systemEquip.fanLoopOutput.readHisVal(),
                    ahuSettings.systemEquip.fanPressureLoopOutput.readHisVal()
            )
            AdvancedAhuRelayAssociationType.OCCUPIED_ENABLE -> getOccupiedEnableRelayState()
            AdvancedAhuRelayAssociationType.AHU_FRESH_AIR_FAN_COMMAND -> getAhuFreshAirFanRunCommandRelayState(
                    systemOccupied, ahuSettings.systemEquip.co2LoopOutput.readHisVal())
            AdvancedAhuRelayAssociationType.EXHAUST_FAN -> getExhaustFan1CommandRelayState(ahuSettings, stageIndex)
            else -> false
        }
        return Pair(associatedPoint, pointVal)
    }

    fun getAnalogLogicalPhysicalValue(
            enable: Point,
            association: Point,
            ahuSettings: AhuSettings,
            systemEquip: DomainEquip
    ) : Pair<Double,Double> {
        return when (systemEquip) {
            is AdvancedHybridSystemEquip -> {
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[association.readDefaultVal().toInt()]
                    CcuLog.i(L.TAG_CCU_SYSTEM, "getAnalogOutValue- association: ${association.domainName}, analogOutAssociationType: $analogOutAssociationType")
                Pair (
                        getAnalogOutValueForLoopType(enable, analogOutAssociationType, ahuSettings),
                        getLogicalOutput(analogOutAssociationType, enable, ahuSettings)
                )

            }
            is ConnectModuleEquip -> {
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationTypeConnect.values()[association.readDefaultVal().toInt()]
                CcuLog.i(L.TAG_CCU_SYSTEM, "getAnalogOutValue- association: ${association.domainName}, analogOutAssociationType: $analogOutAssociationType")
                Pair (
                        getConnectAnalogOutValueForLoopType(enable, analogOutAssociationType, ahuSettings),
                        getConnectLogicalOutput(analogOutAssociationType, enable, ahuSettings)
                )
            }
            else -> {
                throw IllegalArgumentException("Invalid system equip type")
            }
        }
    }



    fun getEnabledAnalogControls(systemEquip: AdvancedHybridSystemEquip? = null, connectEquip1: ConnectModuleEquip? = null) : Set<AdvancedAhuAnalogOutAssociationType> {
        val enabledControls = mutableSetOf<AdvancedAhuAnalogOutAssociationType>()
        if (systemEquip != null) {
            getCMAnalogAssociationMap(systemEquip).forEach { (analogOut: Point, association: Point) ->
                if (analogOut.readDefaultVal() > 0) { // is config enabled
                    val analogOutAssociationType =
                        AdvancedAhuAnalogOutAssociationType.values()[association.readDefaultVal()
                            .toInt()]
                    enabledControls.add(analogOutAssociationType)
                }
            }
        }
        if (connectEquip1 != null) {
            getAnalogAssociation(enabledControls, connectEquip1)
        }
        return enabledControls
    }

    private fun isEmergencyShutOffEnabledAndActivated(systemEquip: AdvancedHybridSystemEquip): Boolean {
        if (systemEquip.thermistor1InputEnable.readDefaultVal() == 1.0 &&
            isMappedToEmergencyShutoff(systemEquip.thermistor1InputAssociation.readDefaultVal().toInt())) {
            val mapping = thermistorAssociationDomainName(systemEquip.thermistor1InputAssociation.readDefaultVal().toInt(), systemEquip)
            if (mapping != null && mapping.readHisVal() == 1.0) {
                return true
            }
        }
        if (systemEquip.thermistor2InputEnable.readDefaultVal() == 1.0 &&
            isMappedToEmergencyShutoff(systemEquip.thermistor2InputAssociation.readDefaultVal().toInt())) {
            val mapping = thermistorAssociationDomainName(systemEquip.thermistor2InputAssociation.readDefaultVal().toInt(), systemEquip)
            if (mapping != null && mapping.readHisVal() == 1.0) {
                return true
            }
        }
        return false
    }

    fun isEmergencyShutOffEnabledAndActive(
            systemEquip: AdvancedHybridSystemEquip ?= null, connectEquip1: ConnectModuleEquip? = null
    ): Boolean {

        if ((systemEquip != null) && isEmergencyShutOffEnabledAndActivated(systemEquip)) {
            return true
        }
        if ((connectEquip1 != null) && isConnectEmergencyShutOffEnabledAndActivated(connectEquip1)) {
            return true
        }
        return false
    }

    private fun isReheatActive(hayStack: CCUHsApi): Boolean {
        val reheatPoints = hayStack
            .readAllEntities("domainName == \""+ DomainName.reheatCmd+"\"")

        for (point in reheatPoints) {
            val reheatPos = hayStack.readHisValById(point["id"].toString())
            if (reheatPos > 0.01) {
                CcuLog.i(L.TAG_CCU_SYSTEM, "Reheat Active and requires AHU Fan")
                return true
            }
        }
        return false
    }
}