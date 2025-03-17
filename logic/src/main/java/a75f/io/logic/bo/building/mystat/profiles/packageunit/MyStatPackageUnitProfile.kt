package a75f.io.logic.bo.building.mystat.profiles.packageunit

import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hvac.AnalogOutput
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.mystat.profiles.MyStatProfile
import a75f.io.logic.bo.building.mystat.profiles.util.updateLogicalPoint

/**
 * Created by Manjunath K on 17-01-2025.
 */

abstract class MyStatPackageUnitProfile: MyStatProfile() {


    fun doCoolingStage1(port: Port, coolingLoopOutput: Int, relayActivationHysteresis: Int, relayStages: HashMap<String, Int>) {
        var relayState = -1.0
        if (coolingLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (coolingLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.COOLING_1.displayName] = 1
        }
    }

    fun doCoolingStage2(port: Port, coolingLoopOutput: Int, relayActivationHysteresis: Int, divider: Int, relayStages: HashMap<String, Int>) {
        var relayState = -1.0
        if (coolingLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (coolingLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.COOLING_2.displayName] = 1
        }
    }

    fun doHeatingStage1(port: Port, heatingLoopOutput: Int, relayActivationHysteresis: Int, relayStages: HashMap<String, Int>) {
        var relayState = -1.0
        if (heatingLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (heatingLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.HEATING_1.displayName] = 1
        }
    }

    fun doHeatingStage2(
        port: Port, heatingLoopOutput: Int, relayActivationHysteresis: Int,
        divider: Int, relayStages: HashMap<String, Int>
    ) {
        var relayState = -1.0
        if (heatingLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (heatingLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            relayStages[Stage.HEATING_2.displayName] = 1
        }
    }

    fun doCompressorStage1(port: Port, compressorLoopOutput: Int, relayActivationHysteresis: Int, relayStages: HashMap<String, Int>, zoneMode: ZoneState) {
        var relayState = -1.0
        if (compressorLoopOutput > relayActivationHysteresis)
            relayState = 1.0
        if (compressorLoopOutput == 0)
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            if (zoneMode == ZoneState.COOLING)
                relayStages[Stage.COOLING_1.displayName] = 1
            if (zoneMode == ZoneState.HEATING)
                relayStages[Stage.HEATING_1.displayName] = 1
        }
    }

    fun doCompressorStage2(
        port: Port, compressorLoopOutput: Int, relayActivationHysteresis: Int,
        divider: Int, relayStages: HashMap<String, Int>, zoneMode: ZoneState
    ) {
        var relayState = -1.0
        if (compressorLoopOutput > (divider + (relayActivationHysteresis / 2)))
            relayState = 1.0
        if (compressorLoopOutput <= (divider - (relayActivationHysteresis / 2)))
            relayState = 0.0
        if (relayState != -1.0) {
            updateLogicalPoint(logicalPointsList[port]!!, relayState)
        }
        if (getCurrentLogicalPointStatus(logicalPointsList[port]!!) == 1.0) {
            if (zoneMode == ZoneState.COOLING)
                relayStages[Stage.COOLING_2.displayName] = 1
            if (zoneMode == ZoneState.HEATING)
                relayStages[Stage.HEATING_2.displayName] = 1
        }
    }

    fun doAnalogCooling(port: Port, conditioningMode: StandaloneConditioningMode, analogOutStages: HashMap<String, Int>, coolingLoopOutput: Int) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.COOL_ONLY.ordinal || conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal) {
            updateLogicalPoint(logicalPointsList[port]!!, coolingLoopOutput.toDouble())
            if (coolingLoopOutput > 0) analogOutStages[AnalogOutput.COOLING.name] = coolingLoopOutput
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogHeating(
        port: Port,
        conditioningMode: StandaloneConditioningMode,
        analogOutStages: HashMap<String, Int>,
        heatingLoopOutput: Int
    ) {
        if (conditioningMode.ordinal == StandaloneConditioningMode.HEAT_ONLY.ordinal ||
            conditioningMode.ordinal == StandaloneConditioningMode.AUTO.ordinal
        ) {
            updateLogicalPoint(logicalPointsList[port]!!, heatingLoopOutput.toDouble())
            if (heatingLoopOutput > 0) analogOutStages[AnalogOutput.HEATING.name] =
                heatingLoopOutput
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

    fun doAnalogCompressorSpeed(port: Port, conditioningMode: StandaloneConditioningMode, analogOutStages: HashMap<String, Int>, compressorLoopOutput: Int, zoneMode: ZoneState) {
        if (conditioningMode != StandaloneConditioningMode.OFF) {
            updateLogicalPoint(logicalPointsList[port]!!, compressorLoopOutput.toDouble())
            if (compressorLoopOutput > 0) {
                if (zoneMode == ZoneState.COOLING) analogOutStages[AnalogOutput.COOLING.name] = compressorLoopOutput
                if (zoneMode == ZoneState.HEATING) analogOutStages[AnalogOutput.HEATING.name] = compressorLoopOutput
            }
        } else {
            updateLogicalPoint(logicalPointsList[port]!!, 0.0)
        }
    }

}