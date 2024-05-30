package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Point
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.SystemEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.oao.OAOProfile

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

    private fun getFanRelayState(stageIndex: Int, point: Point, systemFanLoop: Double, relayHysteresis: Double, fanStagesAvailable : Int, isSystemOccupied: Boolean) : Boolean{
        return when(stageIndex) {
            0 -> (isSystemOccupied || systemFanLoop > 0 || isReheatActive(CCUHsApi.getInstance()))
            1 -> systemFanLoop > 0
            else -> {
                val stageThreshold = 100 * (stageIndex - 1) / fanStagesAvailable -1
                val currentState = point.readHisVal()
                CcuLog.i(
                    L.TAG_CCU_SYSTEM, "stageIndex $stageIndex getFanRelayState: currentState: " +
                            "$currentState, stageThreshold: $stageThreshold, systemHeatingLoop: " +
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

    private fun getFanEnableRelayState(systemCoolingLoop: Double, systemHeatingLoop: Double) : Boolean{
        return systemCoolingLoop > 0 || systemHeatingLoop > 0
    }

    private fun getOccupiedEnableRelayState(isSystemOccupied: Boolean, systemCoolingLoop: Double, systemHeatingLoop: Double, systemFanLoop: Double) : Boolean{
        return isSystemOccupied || systemCoolingLoop > 0 || systemHeatingLoop > 0 || systemFanLoop > 0
    }

    private fun getAhuFreshAirFanRunCommandRelayState(isSystemOccupied: Boolean, systemCo2Loop : Double) : Boolean{
        return isSystemOccupied && systemCo2Loop > 0
    }


    fun getAdvancedAhuRelayState(association : Point, systemEquip: VavAdvancedHybridSystemEquip, coolingStages: Int, heatingStages: Int,
                                 fanStages: Int, systemOccupied : Boolean, isConnectEquip : Boolean): Pair<Point, Boolean> {

        val associatedPointName = relayAssociationToDomainName(association.readDefaultVal().toInt())
        //Get logical point from association index
        val associatedPoint = if (isConnectEquip) {
            getDomainPointForName(associatedPointName, systemEquip.connectEquip1)
        } else {
            getDomainPointForName(associatedPointName, systemEquip)
        }
        val stageIndex = getStageIndex(associatedPoint)
        CcuLog.i(L.TAG_CCU_SYSTEM, "getRelayState: associatedPoint: $associatedPointName, stage: $stageIndex ")
        val pointVal =  when (relayAssociationDomainNameToType(associatedPointName)) {
            AdvancedAhuRelayAssociationType.LOAD_COOLING -> if (systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) {
                getCoolingRelayState(
                    stageIndex,
                    associatedPoint,
                    systemEquip.coolingLoopOutput.readHisVal(),
                    L.ccu().oaoProfile,
                    systemEquip.vavRelayDeactivationHysteresis.readHisVal(),
                    coolingStages
                )
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalCooling Not available")
                false
            }

            AdvancedAhuRelayAssociationType.LOAD_HEATING -> if (systemEquip.mechanicalHeatingAvailable.readHisVal() > 0) {
                getHeatingRelayState(
                    stageIndex, associatedPoint, systemEquip.heatingLoopOutput.readHisVal(),
                    systemEquip.vavRelayDeactivationHysteresis.readHisVal(), heatingStages
                )
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalHeating Not available")
                false
            }

            AdvancedAhuRelayAssociationType.LOAD_FAN -> getFanRelayState(stageIndex, associatedPoint, systemEquip.fanLoopOutput.readHisVal(),
                systemEquip.vavRelayDeactivationHysteresis.readHisVal(), fanStages, systemOccupied)

            AdvancedAhuRelayAssociationType.HUMIDIFIER -> {
                if (systemOccupied && systemEquip.conditioningMode.readPriorityVal() > 0) {
                    getHumidifierRelayState(associatedPoint, systemEquip.averageHumidity.readHisVal(),
                        systemEquip.systemtargetMinInsideHumidity.readHisVal(), systemEquip.vavHumidityHysteresis.readHisVal())
                } else {
                    false
                }
            }

            AdvancedAhuRelayAssociationType.DEHUMIDIFIER ->
                if (systemOccupied && systemEquip.conditioningMode.readPriorityVal() > 0) {
                    getDehumidifierRelayState(associatedPoint, systemEquip.averageHumidity.readHisVal(),
                        systemEquip.systemtargetMaxInsideHumidity.readHisVal(), systemEquip.vavHumidityHysteresis.readHisVal())
                } else {
                    false
                }

            AdvancedAhuRelayAssociationType.SAT_COOLING -> if (systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) {
                getCoolingRelayState(
                    stageIndex,
                    associatedPoint,
                    systemEquip.satCoolingLoopOutput.readHisVal(),
                    L.ccu().oaoProfile,
                    systemEquip.vavRelayDeactivationHysteresis.readHisVal(),
                    coolingStages
                )
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalCooling Not available")
                false
            }

            AdvancedAhuRelayAssociationType.SAT_HEATING -> if (systemEquip.mechanicalHeatingAvailable.readHisVal() > 0) {
                getHeatingRelayState(stageIndex, associatedPoint, systemEquip.satHeatingLoopOutput.readHisVal(),
                    systemEquip.vavRelayDeactivationHysteresis.readHisVal(), heatingStages)
            } else {
                CcuLog.i(L.TAG_CCU_SYSTEM, "mechanicalHeating Not available")
                false
            }

            AdvancedAhuRelayAssociationType.FAN_PRESSURE -> getFanRelayState(stageIndex, associatedPoint, systemEquip.fanPressureLoopOutput.readHisVal(),
                systemEquip.vavRelayDeactivationHysteresis.readHisVal(), fanStages, systemOccupied)

            AdvancedAhuRelayAssociationType.FAN_ENABLE -> getFanEnableRelayState(systemEquip.coolingLoopOutput.readHisVal(),
                                                            systemEquip.heatingLoopOutput.readHisVal())
            AdvancedAhuRelayAssociationType.OCCUPIED_ENABLE -> getOccupiedEnableRelayState(systemOccupied, systemEquip.coolingLoopOutput.readHisVal(),
                                                            systemEquip.heatingLoopOutput.readHisVal(), systemEquip.fanLoopOutput.readHisVal())
            AdvancedAhuRelayAssociationType.AHU_FRESH_AIR_FAN_COMMAND -> getAhuFreshAirFanRunCommandRelayState(systemOccupied, systemEquip.co2LoopOutput.readHisVal())
        }
        return Pair(associatedPoint, pointVal)
    }

    fun getAnalogLogicalPhysicalValue(enable: Point, association: Point, systemEquip: DomainEquip, mode: SystemMode) : Pair<Double,Double> {
        return when (systemEquip) {
            is VavAdvancedHybridSystemEquip -> {
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[association.readDefaultVal().toInt()]
                    CcuLog.i(L.TAG_CCU_SYSTEM, "getAnalogOutValue- association: ${association.domainName}, analogOutAssociationType: $analogOutAssociationType")
                Pair (
                        getAnalogOutValueForLoopType(enable, systemEquip, analogOutAssociationType),
                        getLogicalOutput(systemEquip, analogOutAssociationType,enable, mode)
                )

            }
            is ConnectModuleEquip -> {
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationTypeConnect.values()[association.readDefaultVal().toInt()]
                CcuLog.i(L.TAG_CCU_SYSTEM, "getAnalogOutValue- association: ${association.domainName}, analogOutAssociationType: $analogOutAssociationType")
                Pair (
                        getConnectAnalogOutValueForLoopType(enable, systemEquip, equip, analogOutAssociationType),
                        getConnectLogicalOutput(systemEquip, analogOutAssociationType, enable, mode)
                )
            }
            else -> {
                throw IllegalArgumentException("Invalid system equip type")
            }
        }
    }



    fun getEnabledAnalogControls() : Set<AdvancedAhuAnalogOutAssociationType> {
        val enabledControls = mutableSetOf<AdvancedAhuAnalogOutAssociationType>()
        getCMAnalogAssociationMap(equip).forEach { (analogOut: Point, association: Point) ->
            if (analogOut.readDefaultVal() > 0) { // is config enabled
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[association.readDefaultVal().toInt()]
                enabledControls.add(analogOutAssociationType)
            }
        }
        getAnalogAssociation(enabledControls, equip)
        return enabledControls
    }

    private fun isEmergencyShutOffEnabledAndActivated(systemEquip: VavAdvancedHybridSystemEquip): Boolean {
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

    fun isEmergencyShutOffEnabledAndActive(): Boolean {
        if (isEmergencyShutOffEnabledAndActivated(equip as VavAdvancedHybridSystemEquip)) {
            return true
        }
        if (isConnectEmergencyShutOffEnabledAndActivated(equip.connectEquip1)) {
            return true
        }
        return false
    }

    private fun isReheatActive(hayStack: CCUHsApi): Boolean {
        val reheatPoints = hayStack
            .readAllEntities("point and vav and reheat and cmd")

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