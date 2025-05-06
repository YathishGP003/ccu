package a75f.io.logic.bo.building.system

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.equips.AdvancedHybridSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.util.AhuSettings
import a75f.io.logic.bo.building.system.util.getComposeMidPoint
import a75f.io.logic.bo.building.system.util.getModulatedOutput
import a75f.io.logic.bo.building.system.util.getModulatedOutputDuringEcon

fun getCMRelayLogicalPhysicalMap(systemEquip: AdvancedHybridSystemEquip): Map<Point, PhysicalPoint> {
    val map: MutableMap<Point, PhysicalPoint> = java.util.HashMap()
    map[systemEquip.relay1OutputEnable] = Domain.cmBoardDevice.relay1
    map[systemEquip.relay2OutputEnable] = Domain.cmBoardDevice.relay2
    map[systemEquip.relay3OutputEnable] = Domain.cmBoardDevice.relay3
    map[systemEquip.relay4OutputEnable] = Domain.cmBoardDevice.relay4
    map[systemEquip.relay5OutputEnable] = Domain.cmBoardDevice.relay5
    map[systemEquip.relay6OutputEnable] = Domain.cmBoardDevice.relay6
    map[systemEquip.relay7OutputEnable] = Domain.cmBoardDevice.relay7
    map[systemEquip.relay8OutputEnable] = Domain.cmBoardDevice.relay8
    return map
}
fun getAnalogOutLogicalPhysicalMap(systemEquip: AdvancedHybridSystemEquip) : Map<Point, PhysicalPoint> {
    val map: MutableMap<Point, PhysicalPoint> = HashMap()
    map[systemEquip.analog1OutputEnable] = Domain.cmBoardDevice.analog1Out
    map[systemEquip.analog2OutputEnable] = Domain.cmBoardDevice.analog2Out
    map[systemEquip.analog3OutputEnable] = Domain.cmBoardDevice.analog3Out
    map[systemEquip.analog4OutputEnable] = Domain.cmBoardDevice.analog4Out

    return map
}

fun getAnalogOutValueForLoopType(
    enable: Point,
    controlType: AdvancedAhuAnalogOutAssociationType,
    ahuSettings: AhuSettings,
    isLockoutActiveDuringUnoccupied: Boolean
) : Double {
    val loopOutput = getCmLoopOutput(ahuSettings.systemEquip, controlType, enable)
    return when (enable.domainName) {
        DomainName.analog1OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, getAnalogOut1MinMax(controlType, ahuSettings.systemEquip), ahuSettings, isLockoutActiveDuringUnoccupied)
        }
        DomainName.analog2OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, getAnalogOut2MinMax(controlType, ahuSettings.systemEquip), ahuSettings, isLockoutActiveDuringUnoccupied)
        }
        DomainName.analog3OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, getAnalogOut3MinMax(controlType, ahuSettings.systemEquip), ahuSettings, isLockoutActiveDuringUnoccupied)
        }
        DomainName.analog4OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, getAnalogOut4MinMax(controlType, ahuSettings.systemEquip), ahuSettings, isLockoutActiveDuringUnoccupied)
        }
        else -> 0.0
    }
}

fun getLogicalOutput(
        controlType: AdvancedAhuAnalogOutAssociationType,
        source: Point,
        ahuSettings: AhuSettings
) : Double {
    val loopOutput = getCmLoopOutput(ahuSettings.systemEquip, controlType, source)
    val minMax = getMinMax(source, controlType, ahuSettings.systemEquip)
    return when (controlType) {
        AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL -> {
            if (ahuSettings.isMechanicalCoolingAvailable
                    || ahuSettings.isMechanicalHeatingAvailable
                    || ahuSettings.isEmergencyShutoffActive) {
                return getComposeMidPoint(minMax) * 10
            }
            when (ahuSettings.conditioningMode) {
                SystemMode.COOLONLY, SystemMode.HEATONLY -> { minMax.first * 10 }
                SystemMode.AUTO -> { getModulatedOutput(loopOutput, minMax.first, minMax.second) * 10 }
                else -> { loopOutput }
            }
        }
        AdvancedAhuAnalogOutAssociationType.LOAD_COOLING, AdvancedAhuAnalogOutAssociationType.SAT_COOLING -> {
            if (ahuSettings.isMechanicalCoolingAvailable) {
                0.0
            } else {
                loopOutput
            }
        }
        AdvancedAhuAnalogOutAssociationType.LOAD_HEATING, AdvancedAhuAnalogOutAssociationType.SAT_HEATING -> {
            if (ahuSettings.isMechanicalHeatingAvailable) {
                0.0
            } else {
                loopOutput
            }
        }
        else -> {
            loopOutput
        }
    }
}

fun getCmLoopOutput(systemEquip: AdvancedHybridSystemEquip, controlType: AdvancedAhuAnalogOutAssociationType, source: Point) : Double {
    return when (controlType) {
        AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN -> systemEquip.fanPressureLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationType.SAT_COOLING -> systemEquip.satCoolingLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationType.SAT_HEATING -> systemEquip.satHeatingLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationType.LOAD_COOLING -> systemEquip.coolingLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationType.LOAD_HEATING -> systemEquip.heatingLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationType.LOAD_FAN -> systemEquip.fanLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationType.CO2_DAMPER -> systemEquip.co2LoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    systemEquip.coolingLoopOutput.readHisVal()
                }
                SystemController.State.HEATING -> {
                    systemEquip.heatingLoopOutput.readHisVal()
                }
                else -> {
                  return getComposeMidPoint(getMinMax(source, controlType, systemEquip)) * 10
                }
            }
        }
        // Just to avoid build errors for now. OAO and RETURN dampers are unused in the Advanced AHU system profiles
        AdvancedAhuAnalogOutAssociationType.OAO_DAMPER -> 0.0
        AdvancedAhuAnalogOutAssociationType.RETURN_DAMPER -> 0.0
    }
}

fun getMinMax(source: Point, controlType: AdvancedAhuAnalogOutAssociationType, systemEquip: AdvancedHybridSystemEquip): Pair<Double, Double>{
   return when(source.domainName) {
        DomainName.analog1OutputEnable -> getAnalogOut1MinMax(controlType, systemEquip)
        DomainName.analog2OutputEnable -> getAnalogOut2MinMax(controlType, systemEquip)
        DomainName.analog3OutputEnable -> getAnalogOut3MinMax(controlType, systemEquip)
        DomainName.analog4OutputEnable -> getAnalogOut4MinMax(controlType, systemEquip)
       else -> { Pair(0.0, 0.0)}
   }
}

fun getAnalogOut1MinMax(controlType : AdvancedAhuAnalogOutAssociationType, systemEquip: AdvancedHybridSystemEquip) : Pair<Double, Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0

    when(controlType) {
        AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN -> {
            analogMinVoltage = systemEquip.analog1MinStaticPressure.readDefaultVal()
            analogMaxVoltage = systemEquip.analog1MaxStaticPressure.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.SAT_COOLING -> {
            analogMinVoltage = systemEquip.analog1MinSatCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog1MaxSatCooling.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.SAT_HEATING -> {
            analogMinVoltage = systemEquip.analog1MinSatHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog1MaxSatHeating.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.LOAD_COOLING -> {
            analogMinVoltage = systemEquip.analog1MinCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog1MaxCooling.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.LOAD_HEATING -> {
            analogMinVoltage = systemEquip.analog1MinHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog1MaxHeating.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.LOAD_FAN -> {
            analogMinVoltage = systemEquip.analog1MinFan.readDefaultVal()
            analogMaxVoltage = systemEquip.analog1MaxFan.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.CO2_DAMPER -> {
            analogMinVoltage = systemEquip.analog1MinDamperPos.readDefaultVal()
            analogMaxVoltage = systemEquip.analog1MaxDamperPos.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = systemEquip.analog1MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog1MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = systemEquip.analog1MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog1MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = systemEquip.analog1MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog1MinHeatingComposite.readDefaultVal()
                }
            }
        }
        // Just to avoid build errors for now. OAO and RETURN dampers are unused in the Advanced AHU system profiles
        AdvancedAhuAnalogOutAssociationType.OAO_DAMPER -> {
        }
        AdvancedAhuAnalogOutAssociationType.RETURN_DAMPER -> {
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getAnalogOut2MinMax(controlType : AdvancedAhuAnalogOutAssociationType, systemEquip: AdvancedHybridSystemEquip) : Pair<Double, Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0
    when (controlType) {
        AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN -> {
            analogMinVoltage = systemEquip.analog2MinStaticPressure.readDefaultVal()
            analogMaxVoltage = systemEquip.analog2MaxStaticPressure.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.SAT_COOLING -> {
            analogMinVoltage = systemEquip.analog2MinSatCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog2MaxSatCooling.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.SAT_HEATING -> {
            analogMinVoltage = systemEquip.analog2MinSatHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog2MaxSatHeating.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_COOLING -> {
            analogMinVoltage = systemEquip.analog2MinCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog2MaxCooling.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_HEATING -> {
            analogMinVoltage = systemEquip.analog2MinHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog2MaxHeating.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_FAN -> {
            analogMinVoltage = systemEquip.analog2MinFan.readDefaultVal()
            analogMaxVoltage = systemEquip.analog2MaxFan.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.CO2_DAMPER -> {
            analogMinVoltage = systemEquip.analog2MinDamperPos.readDefaultVal()
            analogMaxVoltage = systemEquip.analog2MaxDamperPos.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = systemEquip.analog2MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog2MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = systemEquip.analog2MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog2MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = systemEquip.analog2MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog2MinHeatingComposite.readDefaultVal()
                }
            }
        }
        // Just to avoid build errors for now. OAO and RETURN dampers are unused in the Advanced AHU system profiles
        AdvancedAhuAnalogOutAssociationType.OAO_DAMPER -> {
        }
        AdvancedAhuAnalogOutAssociationType.RETURN_DAMPER -> {
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getAnalogOut3MinMax(controlType : AdvancedAhuAnalogOutAssociationType, systemEquip: AdvancedHybridSystemEquip) : Pair<Double, Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0
    when (controlType) {
        AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN -> {
            analogMinVoltage = systemEquip.analog3MinStaticPressure.readDefaultVal()
            analogMaxVoltage = systemEquip.analog3MaxStaticPressure.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.SAT_COOLING -> {
            analogMinVoltage = systemEquip.analog3MinSatCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog3MaxSatCooling.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.SAT_HEATING -> {
            analogMinVoltage = systemEquip.analog3MinSatHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog3MaxSatHeating.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_COOLING -> {
            analogMinVoltage = systemEquip.analog3MinCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog3MaxCooling.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_HEATING -> {
            analogMinVoltage = systemEquip.analog3MinHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog3MaxHeating.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_FAN -> {
            analogMinVoltage = systemEquip.analog3MinFan.readDefaultVal()
            analogMaxVoltage = systemEquip.analog3MaxFan.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.CO2_DAMPER -> {
            analogMinVoltage = systemEquip.analog3MinDamperPos.readDefaultVal()
            analogMaxVoltage = systemEquip.analog3MaxDamperPos.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = systemEquip.analog3MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog3MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = systemEquip.analog3MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog3MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = systemEquip.analog3MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog3MinHeatingComposite.readDefaultVal()
                }
            }
        }
        // Just to avoid build errors for now. OAO and RETURN dampers are unused in the Advanced AHU system profiles
        AdvancedAhuAnalogOutAssociationType.OAO_DAMPER -> {
        }
        AdvancedAhuAnalogOutAssociationType.RETURN_DAMPER -> {
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getAnalogOut4MinMax(controlType : AdvancedAhuAnalogOutAssociationType, systemEquip: AdvancedHybridSystemEquip) : Pair<Double, Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0
    when (controlType) {
        AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN -> {
            analogMinVoltage = systemEquip.analog4MinStaticPressure.readDefaultVal()
            analogMaxVoltage = systemEquip.analog4MaxStaticPressure.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.SAT_COOLING -> {
            analogMinVoltage = systemEquip.analog4MinSatCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog4MaxSatCooling.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.SAT_HEATING -> {
            analogMinVoltage = systemEquip.analog4MinSatHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog4MaxSatHeating.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_COOLING -> {
            analogMinVoltage = systemEquip.analog4MinCooling.readDefaultVal()
            analogMaxVoltage = systemEquip.analog4MaxCooling.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_HEATING -> {
            analogMinVoltage = systemEquip.analog4MinHeating.readDefaultVal()
            analogMaxVoltage = systemEquip.analog4MaxHeating.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_FAN -> {
            analogMinVoltage = systemEquip.analog4MinFan.readDefaultVal()
            analogMaxVoltage = systemEquip.analog4MaxFan.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.CO2_DAMPER -> {
            analogMinVoltage = systemEquip.analog4MinDamperPos.readDefaultVal()
            analogMaxVoltage = systemEquip.analog4MaxDamperPos.readDefaultVal()
        }

        AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = systemEquip.analog4MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog4MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = systemEquip.analog4MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog4MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = systemEquip.analog4MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = systemEquip.analog4MinHeatingComposite.readDefaultVal()
                }
            }
        }
        // Just to avoid build errors for now. OAO and RETURN dampers are unused in the Advanced AHU system profiles
        AdvancedAhuAnalogOutAssociationType.OAO_DAMPER -> {
        }
        AdvancedAhuAnalogOutAssociationType.RETURN_DAMPER -> {
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getAnalogModulation(
        loopOutput: Double,
        controlType: AdvancedAhuAnalogOutAssociationType,
        minMax: Pair<Double, Double>,
        ahuSettings: AhuSettings,
        isLockoutActiveDuringUnoccupied: Boolean
) : Double {
    var econFlag : Boolean = false
    val finalLoop = when (controlType) {
        AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL -> {
            if (ahuSettings.isMechanicalCoolingAvailable || ahuSettings.isMechanicalHeatingAvailable
                    || ahuSettings.isEmergencyShutoffActive) {
                CcuLog.i(L.TAG_CCU_SYSTEM, " compositeSignal ${getComposeMidPoint(minMax)} analogMinVoltage: ${minMax.first}, analogMaxVoltage: ${minMax.second}")
                return getComposeMidPoint(minMax) * 10
            }
            val presentMode = SystemController.State.values()[ahuSettings.systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    ahuSettings.systemEquip.coolingLoopOutput.readHisVal()
                }
                SystemController.State.HEATING -> {
                    ahuSettings.systemEquip.heatingLoopOutput.readHisVal()
                }
                else -> {
                    CcuLog.i(L.TAG_CCU_SYSTEM, " compositeSignal ${getComposeMidPoint(minMax)} analogMinVoltage: ${minMax.first}, analogMaxVoltage: ${minMax.second}")
                    return getComposeMidPoint(minMax) * 10
                }
            }
        }
        AdvancedAhuAnalogOutAssociationType.LOAD_COOLING ,AdvancedAhuAnalogOutAssociationType.SAT_COOLING -> {
            if (ahuSettings.isMechanicalCoolingAvailable) {
                0.0
            } else {
                if(L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable) {
                    econFlag = true
                }
                loopOutput
            }
        }
        AdvancedAhuAnalogOutAssociationType.LOAD_HEATING ,AdvancedAhuAnalogOutAssociationType.SAT_HEATING -> {
            if (ahuSettings.isMechanicalHeatingAvailable) {
                0.0
            } else {
                loopOutput
            }
        }

        AdvancedAhuAnalogOutAssociationType.LOAD_FAN ,AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN -> {
            if (isLockoutActiveDuringUnoccupied) {
                0.0
            } else {
                loopOutput
            }
        }
        else -> {
            loopOutput
        }
    }
    CcuLog.i(L.TAG_CCU_SYSTEM, "modulateAnalogOut: loopOutput $finalLoop analogMinVoltage: ${minMax.first}, analogMaxVoltage: ${minMax.second}")
    return if (econFlag) {
        // When econ is on we need to send get different modulated output for analog outs
        val economizingToMainCoolingLoopMap = L.ccu().oaoProfile.oaoEquip.economizingToMainCoolingLoopMap.readPriorityVal()
        getModulatedOutputDuringEcon(finalLoop, minMax.first, minMax.second, economizingToMainCoolingLoopMap).coerceIn(0.0, 10.0) * 10
    } else {
        getModulatedOutput(finalLoop, minMax.first, minMax.second).coerceIn(0.0, 10.0) * 10
    }
}




