package a75f.io.logic.bo.building.system

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.util.AhuSettings
import a75f.io.logic.bo.building.system.util.getComposeMidPoint
import a75f.io.logic.bo.building.system.util.getModulatedOutput


fun getAnalogAssociation(enabledControls: MutableSet<AdvancedAhuAnalogOutAssociationType>, connectEquip1: ConnectModuleEquip) {

    /**
     * Connect module Analog association enum is not same as cm model enum.
     * so this function adds cm model analog map to based on the cooling heating enabled in connect module
     */
    getConnectAnalogAssociationMap(connectEquip1).forEach { (analogOut: Point, association: Point) ->
        if (analogOut.readDefaultVal() > 0) { // is config enabled
            when (AdvancedAhuAnalogOutAssociationTypeConnect.values()[association.readDefaultVal().toInt()]) {
                AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> enabledControls.add(AdvancedAhuAnalogOutAssociationType.LOAD_HEATING)
                AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> enabledControls.add(AdvancedAhuAnalogOutAssociationType.LOAD_COOLING)
                AdvancedAhuAnalogOutAssociationTypeConnect.CO2_DAMPER -> enabledControls.add(AdvancedAhuAnalogOutAssociationType.CO2_DAMPER)
                AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> enabledControls.add(AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL)
                else -> { } // Do nothing
            }
        }
    }
}

 fun getConnectAnalogOutValueForLoopType(
         enable: Point,
         controlType: AdvancedAhuAnalogOutAssociationTypeConnect,
         ahuSettings: AhuSettings
 ) : Double {
    val loopOutput = getConnectLoopOutput (ahuSettings.connectEquip1, controlType, enable, ahuSettings)
    return when (enable.domainName) {
        DomainName.analog1OutputEnable -> {
            getConnectAnalogModulation(loopOutput, controlType, getConnectAnalogOut1MinMax(controlType, ahuSettings.connectEquip1,ahuSettings), ahuSettings)
        }
        DomainName.analog2OutputEnable -> {
            getConnectAnalogModulation(loopOutput, controlType, getConnectAnalogOut2MinMax(controlType, ahuSettings.connectEquip1,ahuSettings), ahuSettings)
        }
        DomainName.analog3OutputEnable -> {
            getConnectAnalogModulation(loopOutput, controlType, getConnectAnalogOut3MinMax(controlType, ahuSettings.connectEquip1,ahuSettings), ahuSettings)
        }
        DomainName.analog4OutputEnable -> {
            getConnectAnalogModulation(loopOutput, controlType, getConnectAnalogOut4MinMax(controlType, ahuSettings.connectEquip1,ahuSettings), ahuSettings)
        }
        else -> 0.0
    }
}


fun getConnectLogicalOutput(
        controlType: AdvancedAhuAnalogOutAssociationTypeConnect,
        source: Point,
        ahuSettings: AhuSettings
) : Double {
    val loopOutput = getConnectLoopOutput(ahuSettings.connectEquip1, controlType, source, ahuSettings)
    val minMax = getMinMax(source, controlType, ahuSettings.connectEquip1, ahuSettings)
    return when (controlType) {
        AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> {
            if (ahuSettings.isMechanicalCoolingAvailable || ahuSettings.isMechanicalHeatingAvailable
                    || ahuSettings.isEmergencyShutoffActive) {
                return getComposeMidPoint(minMax) * 10
            }
            when (ahuSettings.conditioningMode) {
                SystemMode.COOLONLY, SystemMode.HEATONLY -> { minMax.first * 10 }
                SystemMode.AUTO -> { getModulatedOutput(loopOutput, minMax.first, minMax.second) * 10 }
                else -> { loopOutput }
            }
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> {
            if (ahuSettings.isMechanicalCoolingAvailable) {
                0.0
            } else {
                loopOutput
            }
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> {
            if (ahuSettings.isMechanicalHeatingAvailable) {
                0.0
            } else {
                loopOutput
            }
        } else -> {
            loopOutput
        }
    }
}


fun getConnectLoopOutput(
        connectEquip: ConnectModuleEquip,
        controlType: AdvancedAhuAnalogOutAssociationTypeConnect,
        source: Point,
        ahuSettings: AhuSettings
) : Double {
    return when (controlType) {
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> connectEquip.coolingLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> connectEquip.heatingLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_FAN -> connectEquip.fanLoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationTypeConnect.CO2_DAMPER -> connectEquip.co2LoopOutput.readHisVal()
        AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[ahuSettings.systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    connectEquip.coolingLoopOutput.readHisVal()
                }
                SystemController.State.HEATING -> {
                    connectEquip.heatingLoopOutput.readHisVal()
                }
                else -> {
                    return getComposeMidPoint(getMinMax(source, controlType, connectEquip, ahuSettings)) * 10
                }
            }
        }
    }
}

fun getMinMax(
        source: Point,
        controlType: AdvancedAhuAnalogOutAssociationTypeConnect,
        connectEquip: ConnectModuleEquip,
        ahuSettings: AhuSettings
): Pair<Double, Double>{
    return when(source.domainName) {
        DomainName.analog1OutputEnable -> getConnectAnalogOut1MinMax(controlType, connectEquip, ahuSettings)
        DomainName.analog2OutputEnable -> getConnectAnalogOut2MinMax(controlType, connectEquip, ahuSettings)
        DomainName.analog3OutputEnable -> getConnectAnalogOut3MinMax(controlType, connectEquip, ahuSettings)
        DomainName.analog4OutputEnable -> getConnectAnalogOut4MinMax(controlType, connectEquip, ahuSettings)
        else -> { Pair(0.0, 0.0)}
    }
}

 fun isConnectEmergencyShutOffEnabledAndActivated(systemEquip: ConnectModuleEquip): Boolean {
    val config = mapOf(
        systemEquip.universalIn1Enable to systemEquip.universalIn1Association,
        systemEquip.universalIn2Enable to systemEquip.universalIn2Association,
        systemEquip.universalIn3Enable to systemEquip.universalIn3Association,
        systemEquip.universalIn4Enable to systemEquip.universalIn4Association,
        systemEquip.universalIn5Enable to systemEquip.universalIn5Association,
        systemEquip.universalIn6Enable to systemEquip.universalIn6Association,
        systemEquip.universalIn7Enable to systemEquip.universalIn7Association,
        systemEquip.universalIn8Enable to systemEquip.universalIn8Association
    )
    config.forEach {
        if (it.key.readDefaultVal() > 0 && isUniversalMappedToEmergencyShutoff(it.value.readDefaultVal().toInt())) {
            val mapping = universalAssociationDomainName(it.value.readDefaultVal().toInt(), systemEquip)
            CcuLog.i(L.TAG_CCU_SYSTEM,"${it.key.domainName} is mapped ${mapping?.domainName} current state ${mapping?.readHisVal()}")
            if (mapping != null && mapping.readHisVal() == 1.0) {
                return true
            }
        }
    }
    return false
}

fun getConnectAnalogOut1MinMax(
        controlType : AdvancedAhuAnalogOutAssociationTypeConnect, connectEquip: ConnectModuleEquip, ahuSettings: AhuSettings
) : Pair<Double, Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0

    when(controlType) {
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> {
            analogMinVoltage = connectEquip.analog1MinCooling.readDefaultVal()
            analogMaxVoltage = connectEquip.analog1MaxCooling.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> {
            analogMinVoltage = connectEquip.analog1MinHeating.readDefaultVal()
            analogMaxVoltage = connectEquip.analog1MaxHeating.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_FAN -> {
            analogMinVoltage = connectEquip.analog1MinFan.readDefaultVal()
            analogMaxVoltage = connectEquip.analog1MaxFan.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.CO2_DAMPER -> {
            analogMinVoltage = connectEquip.analog1MinDamperPos.readDefaultVal()
            analogMaxVoltage = connectEquip.analog1MaxDamperPos.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[ahuSettings.systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = connectEquip.analog1MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog1MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = connectEquip.analog1MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog1MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = connectEquip.analog1MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog1MinHeatingComposite.readDefaultVal()
                }
            }
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getConnectAnalogOut2MinMax(
    controlType: AdvancedAhuAnalogOutAssociationTypeConnect, connectEquip: ConnectModuleEquip, ahuSettings: AhuSettings
): Pair<Double,Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0

    when(controlType) {
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> {
            analogMinVoltage = connectEquip.analog2MinCooling.readDefaultVal()
            analogMaxVoltage = connectEquip.analog2MaxCooling.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> {
            analogMinVoltage = connectEquip.analog2MinHeating.readDefaultVal()
            analogMaxVoltage = connectEquip.analog2MaxHeating.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_FAN -> {
            analogMinVoltage = connectEquip.analog2MinFan.readDefaultVal()
            analogMaxVoltage = connectEquip.analog2MaxFan.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.CO2_DAMPER -> {
            analogMinVoltage = connectEquip.analog2MinDamperPos.readDefaultVal()
            analogMaxVoltage = connectEquip.analog2MaxDamperPos.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[ahuSettings.systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = connectEquip.analog2MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog2MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = connectEquip.analog2MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog2MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = connectEquip.analog2MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog2MinHeatingComposite.readDefaultVal()
                }
            }
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getConnectAnalogOut3MinMax(
    controlType: AdvancedAhuAnalogOutAssociationTypeConnect, connectEquip: ConnectModuleEquip, ahuSettings: AhuSettings
): Pair<Double,Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0

    when(controlType) {
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> {
            analogMinVoltage = connectEquip.analog3MinCooling.readDefaultVal()
            analogMaxVoltage = connectEquip.analog3MaxCooling.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> {
            analogMinVoltage = connectEquip.analog3MinHeating.readDefaultVal()
            analogMaxVoltage = connectEquip.analog3MaxHeating.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_FAN -> {
            analogMinVoltage = connectEquip.analog3MinFan.readDefaultVal()
            analogMaxVoltage = connectEquip.analog3MaxFan.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.CO2_DAMPER -> {
            analogMinVoltage = connectEquip.analog3MinDamperPos.readDefaultVal()
            analogMaxVoltage = connectEquip.analog3MaxDamperPos.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[ahuSettings.systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = connectEquip.analog3MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog3MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = connectEquip.analog3MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog3MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = connectEquip.analog3MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog3MinHeatingComposite.readDefaultVal()
                }
            }
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}

fun getConnectAnalogOut4MinMax(
    controlType: AdvancedAhuAnalogOutAssociationTypeConnect, connectEquip: ConnectModuleEquip, ahuSettings: AhuSettings
): Pair<Double,Double> {
    var analogMinVoltage = 0.0
    var analogMaxVoltage = 0.0

    when(controlType) {
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> {
            analogMinVoltage = connectEquip.analog4MinCooling.readDefaultVal()
            analogMaxVoltage = connectEquip.analog4MaxCooling.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> {
            analogMinVoltage = connectEquip.analog4MinHeating.readDefaultVal()
            analogMaxVoltage = connectEquip.analog4MaxHeating.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_FAN -> {
            analogMinVoltage = connectEquip.analog4MinFan.readDefaultVal()
            analogMaxVoltage = connectEquip.analog4MaxFan.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.CO2_DAMPER -> {
            analogMinVoltage = connectEquip.analog4MinDamperPos.readDefaultVal()
            analogMaxVoltage = connectEquip.analog4MaxDamperPos.readDefaultVal()
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> {
            val presentMode = SystemController.State.values()[ahuSettings.systemEquip.operatingMode.readHisVal().toInt()]
            when (presentMode) {
                SystemController.State.COOLING -> {
                    analogMinVoltage = connectEquip.analog4MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog4MaxCoolingComposite.readDefaultVal()
                }
                SystemController.State.HEATING -> {
                    analogMinVoltage = connectEquip.analog4MinHeatingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog4MaxHeatingComposite.readDefaultVal()
                }
                else -> {
                    analogMinVoltage = connectEquip.analog4MinCoolingComposite.readDefaultVal()
                    analogMaxVoltage = connectEquip.analog4MinHeatingComposite.readDefaultVal()
                }
            }
        }
    }
    return Pair(analogMinVoltage, analogMaxVoltage)
}


fun getConnectAnalogModulation(
        loopOutput: Double,
        controlType: AdvancedAhuAnalogOutAssociationTypeConnect,
        minMax: Pair<Double, Double>,
        ahuSettings: AhuSettings
) : Double {
    val finalLoop = when (controlType) {
        AdvancedAhuAnalogOutAssociationTypeConnect.COMPOSITE_SIGNAL -> {
            if (ahuSettings.isMechanicalCoolingAvailable || ahuSettings.isMechanicalHeatingAvailable
                    || ahuSettings.isEmergencyShutoffActive){
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
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_COOLING -> {
            if (ahuSettings.isMechanicalCoolingAvailable) {
                0.0
            } else {
                ahuSettings.systemEquip.coolingLoopOutput.readHisVal()
            }
        }
        AdvancedAhuAnalogOutAssociationTypeConnect.LOAD_HEATING -> {
            if (ahuSettings.isMechanicalHeatingAvailable) {
                0.0
            } else {
                ahuSettings.systemEquip.heatingLoopOutput.readHisVal()
            }
        }
        else -> {
            loopOutput
        }
    }
    CcuLog.i(L.TAG_CCU_SYSTEM, "modulateAnalogOut: loopOutput $finalLoop analogMinVoltage: ${minMax.first}, analogMaxVoltage: ${minMax.second}")
    return getModulatedOutput(finalLoop, minMax.first, minMax.second).coerceIn(0.0,10.0) * 10
}