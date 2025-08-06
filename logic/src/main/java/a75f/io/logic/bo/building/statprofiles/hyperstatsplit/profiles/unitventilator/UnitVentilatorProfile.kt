package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator

import a75.io.algos.ControlLoop
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.domain.equips.unitVentilator.UnitVentilatorEquip
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitProfile
import a75f.io.logic.bo.building.statprofiles.util.AuxActiveStages
import a75f.io.logic.bo.building.statprofiles.util.BasicSettings
import a75f.io.logic.bo.building.statprofiles.util.ControlVia
import a75f.io.logic.bo.building.statprofiles.util.UvTuners
import a75f.io.logic.bo.building.statprofiles.util.canWeDoHeating
import a75f.io.logic.bo.building.statprofiles.util.getPercentFromVolt
import a75f.io.logic.controlcomponents.util.isSoftOccupied
import kotlin.math.roundToInt

/**
 * Author: Manjunath Kundaragi
 * Created on: 20-07-2025
 */
abstract class UnitVentilatorProfile(equipRef: String, nodeAddress: Short, tag: String) :
    HyperStatSplitProfile(equipRef, nodeAddress, tag) {
    private val saTemperingLoopControl = ControlLoop()
    var controlVia = ControlVia.FACE_AND_BYPASS_DAMPER
    var saTemperingLoopOutput = 0
    var isAuxStage1Active = false
    var isAuxStage2Active = false

    /**
     * This method is used to calculate the SA tempering loop output based on the current and target values.
     * It uses the proportional and integral gains, as well as the proportional spread and integral max timeout
     * defined in the tuner.
     *
     * @param tuner The UvTuners object containing tuning parameters.
     * @param hssEquip The UnitVentilatorEquip object representing the equipment.
     */
    fun calculateSaTemperingLoop(tuner: UvTuners, hssEquip: UnitVentilatorEquip, basicSettings: BasicSettings) {
        if (hssEquip.enableSaTemperingControl.isEnabled() && isSoftOccupied(hssEquip.occupancyMode) && canWeDoHeating(basicSettings.conditioningMode)) {
            saTemperingLoopControl.setProportionalGain(tuner.saTemperingProportionalKFactor)
            saTemperingLoopControl.setIntegralGain(tuner.saTemperingIntegralKFactor)
            saTemperingLoopControl.setProportionalSpread(tuner.saTemperingTemperatureProportionalRange)
            saTemperingLoopControl.setIntegralMaxTimeout(tuner.saTemperingTemperatureIntegralTime)
            val currentSatValue = hssEquip.dischargeAirTemperature.readHisVal()
            getSaTemperingLoopOutput(
                currentValue = currentSatValue,
                targetValue = tuner.saTemperingSetpoint
            )
            logIt("SA Tempering Loop Output: currentSatValue : $currentSatValue saTemperingSetpoint : ${tuner.saTemperingSetpoint}")
        } else {
            saTemperingLoopOutput = 0
        }
    }

    /**
     * This method calculates the SA tempering loop output based on the current and target values.
     * If the current value is less than or equal to the target value, it computes the loop output.
     * Otherwise, it sets the output to zero.
     *
     * @param currentValue The current value of the discharge air temperature.
     * @param targetValue The target setpoint for SA tempering.
     */

    private fun getSaTemperingLoopOutput(
        currentValue: Double, targetValue: Double
    ) {
        saTemperingLoopOutput = if (currentValue <= targetValue) {
            saTemperingLoopControl.getLoopOutput(targetValue, currentValue).toInt()
        } else {
            0
        }
    }

    /**
     * This method operates the SA tempering control based on the current state of the equipment and the tuner settings.
     * It checks if the SA tempering control is enabled and if the cooling loop output is zero.
     * If conditions are met, it activates the heating valves for SA tempering.
     *
     * @param hssEquip The UnitVentilatorEquip object representing the equipment.
     * @param tuner The UvTuners object containing tuning parameters.
     * @param basicSettings The BasicSettings object containing basic configuration settings.
     */
    fun operateSaTempering(
        hssEquip: UnitVentilatorEquip,
        tuner: UvTuners,
        basicSettings: BasicSettings
    ) {
        if (hssEquip.enableSaTemperingControl.isEnabled()
            && noZoneLoad() && canWeDoHeating(basicSettings.conditioningMode)
        ) {

            if (saTemperingLoopOutput > 0) {
                if (hssEquip is Pipe4UVEquip) {
                    if (saTemperingLoopOutput > tuner.relayActivationHysteresis) {
                        hssEquip.hotWaterHeatValve.writeHisVal(1.0)
                        hssEquip.relayStages[StatusMsgKeys.HEATING_VALVE.name] = 1
                    }
                    if (hssEquip.hotWaterModulatingHeatValve.pointExists()) {
                        hssEquip.hotWaterModulatingHeatValve.writeHisVal(saTemperingLoopOutput.toDouble())
                        hssEquip.analogOutStages[StatusMsgKeys.HEATING_VALVE.name] = saTemperingLoopOutput
                    }

                    if (hssEquip.faceBypassDamperCmd.pointExists() && saTemperingLoopOutput > tuner.faceBypassDamperActivationHysteresis) {
                        hssEquip.faceBypassDamperCmd.writeHisVal(1.0)
                    }
                }

                if (hssEquip.fanSignal.pointExists()) {
                    val fanOutRecirculate =
                        getPercentFromVolt(hssEquip.fanOutRecirculate.readDefaultVal().roundToInt())
                    hssEquip.fanSignal.writeHisVal(fanOutRecirculate.toDouble())
                    if (fanOutRecirculate > 0) {
                        hssEquip.analogOutStages[StatusMsgKeys.FAN_SPEED.name] = fanOutRecirculate
                    }
                }
                if (hssEquip.faceBypassDamperModulatingCmd.pointExists()) {
                    hssEquip.faceBypassDamperModulatingCmd.writeHisVal(saTemperingLoopOutput.toDouble())
                }
            } else {
                if (noZoneLoad()) {
                    logIt("SA Tempering Loop Output is zero, no zone load detected. Disabling SA tempering control.")
                    when (hssEquip) {
                        is Pipe4UVEquip -> {
                            hssEquip.hotWaterHeatValve.writeHisVal(0.0)
                            hssEquip.hotWaterModulatingHeatValve.writeHisVal(0.0)

                            if (fanLoopCounter == 0) {
                                if (hssEquip.fanSignal.pointExists()) {
                                    hssEquip.fanSignal.writeHisVal(0.0)
                                }
                                if (hssEquip.faceBypassDamperModulatingCmd.pointExists()) {
                                    hssEquip.faceBypassDamperModulatingCmd.writeHisVal(0.0)
                                }
                                if (hssEquip.faceBypassDamperCmd.pointExists()) {
                                    hssEquip.faceBypassDamperCmd.writeHisVal(0.0)
                                }
                            }
                        }
                        else -> {}
                    }
                } else {
                    logIt("SA Tempering Loop Output is zero but zone load exists, maintaining current state.")
                }
            }
        } else {
            logIt("Sa Tempering Not applicable..")
        }
    }
    /**
     * This method checks if there is no zone load by verifying that both the cooling and heating loop outputs are zero.
     * It is used to determine if the SA tempering control can be activated.
     * @return Boolean indicating whether there is no zone load.
     * SA tempering should not run ad cooling direction. If it is running in heating direction zone load will be the priority
     */
    private fun noZoneLoad() = (coolingLoopOutput == 0 && heatingLoopOutput == 0)

    fun operateAuxBasedFan(equip: UnitVentilatorEquip): String? {
        val auxType = if (isAuxStage1Active && isAuxStage2Active) {
            AuxActiveStages.BOTH
        } else if (isAuxStage2Active) {
            AuxActiveStages.AUX2
        } else if (isAuxStage1Active) {
            AuxActiveStages.AUX1
        } else {
            AuxActiveStages.NONE
        }
        if (auxType != AuxActiveStages.NONE) {
            val sequenceMap = when (auxType) {
                AuxActiveStages.BOTH, AuxActiveStages.AUX2 -> mutableMapOf(
                    equip.fanHighSpeed to Stage.FAN_3.displayName,
                    equip.fanMediumSpeed to Stage.FAN_2.displayName,
                    equip.fanLowSpeed to Stage.FAN_1.displayName,
                    equip.fanLowSpeedVentilation to Stage.FAN_1.displayName,
                    equip.fanEnable to StatusMsgKeys.FAN_ENABLED.name
                )

                AuxActiveStages.AUX1 -> mutableMapOf(
                    equip.fanMediumSpeed to Stage.FAN_2.displayName,
                    equip.fanHighSpeed to Stage.FAN_3.displayName,
                    equip.fanLowSpeed to Stage.FAN_1.displayName,
                    equip.fanLowSpeedVentilation to Stage.FAN_1.displayName,
                    equip.fanEnable to StatusMsgKeys.FAN_ENABLED.name
                )

                else -> emptyMap()
            }
            // Before operating AUX based fan reset all points except fanEnable
            sequenceMap.forEach { (point, statusMsg) ->
                if ((point.domainName != DomainName.fanEnable) && point.pointExists()) {
                    point.writeHisVal(0.0)
                }
                equip.relayStages.remove(statusMsg)
            }
            sequenceMap.forEach { (point, statusMsg) ->
                if (point.pointExists()) {
                    point.writeHisVal(1.0)
                    equip.relayStages[statusMsg] = 1
                    logIt("Operating AUX based fan: ${point.domainName}")
                    return point.domainName
                }
            }
        }
        return null
    }
}