package a75f.io.logic.bo.building.system

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.equips.AdvancedHybridSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.util.composeMappedLoop
import a75f.io.logic.bo.building.system.util.getModulatedOutput

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
        systemEquip: AdvancedHybridSystemEquip,
        controlType: AdvancedAhuAnalogOutAssociationType
) : Double {
    val loopOutput = getCmLoopOutput(systemEquip, controlType, enable)
    return when (enable.domainName) {
        DomainName.analog1OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, systemEquip, getAnalogOut1MinMax(controlType, systemEquip))
        }
        DomainName.analog2OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, systemEquip, getAnalogOut2MinMax(controlType, systemEquip))
        }
        DomainName.analog3OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, systemEquip, getAnalogOut3MinMax(controlType, systemEquip))
        }
        DomainName.analog4OutputEnable -> {
            getAnalogModulation(loopOutput, controlType, systemEquip, getAnalogOut4MinMax(controlType, systemEquip))
        }
        else -> 0.0
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
            if (systemEquip.coolingLoopOutput.readHisVal() > 0) {
                systemEquip.coolingLoopOutput.readHisVal()
            } else if (systemEquip.heatingLoopOutput.readHisVal() > 0) {
                systemEquip.heatingLoopOutput.readHisVal()
            } else {
                return when(source.domainName) {
                    DomainName.analog1OutputEnable -> composeMappedLoop(getAnalogOut1MinMax(controlType, systemEquip))
                    DomainName.analog2OutputEnable -> composeMappedLoop(getAnalogOut2MinMax(controlType, systemEquip))
                    DomainName.analog3OutputEnable -> composeMappedLoop(getAnalogOut3MinMax(controlType, systemEquip))
                    DomainName.analog4OutputEnable -> composeMappedLoop(getAnalogOut4MinMax(controlType, systemEquip))
                    else -> {0.0}
                }
            }
        }
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
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getAnalogModulation(
        loopOutput: Double,
        controlType: AdvancedAhuAnalogOutAssociationType,
        systemEquip: AdvancedHybridSystemEquip,
        minMax: Pair<Double, Double>
) : Double {

    val finalLoop = when (controlType) {
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
                    (10 * (minMax.first + minMax.second) / 2).coerceIn(0.0,10.0)
                }
            }
        }
        else -> {
            loopOutput
        }
    }

    if (controlType == AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL) {
        CcuLog.i(L.TAG_CCU_SYSTEM, "modulateAnalogOut4: compositeSignal ${((minMax.first + minMax.second) / 2).coerceIn(0.0,10.0)} analogMinVoltage: ${minMax.first}, analogMaxVoltage: ${minMax.second}")
        return ((minMax.first + minMax.second) / 2).coerceIn(0.0,10.0)
    } else {
        CcuLog.i(L.TAG_CCU_SYSTEM, "modulateAnalogOut4: loopOutput $finalLoop analogMinVoltage: ${minMax.first}, analogMaxVoltage: ${minMax.second}")
        return getModulatedOutput(finalLoop, minMax.first, minMax.second).coerceIn(0.0,10.0)
    }
}




