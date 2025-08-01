package a75f.io.logic.bo.building.system

import a75f.io.domain.api.Point
import a75f.io.domain.equips.AdvancedHybridSystemEquip
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.SystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.SystemController.State
import a75f.io.logic.bo.building.system.util.AhuSettings

/**
 * Common implementation of utility methods for VAV and DAB advanced AHUs
 */
class AdvancedAhuAlgoHandler (val equip: SystemEquip) {

    fun getAnalogLogicalPhysicalValue(
            enable: Point,
            association: Point,
            ahuSettings: AhuSettings,
            systemEquip: DomainEquip
    ) : Pair<Double,Double> {
        val lockoutActiveDuringUnOccupied = isLockoutActiveDuringUnoccupied(ahuSettings)
         return when (systemEquip) {
            is AdvancedHybridSystemEquip -> {
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[association.readDefaultVal().toInt()]
                    CcuLog.i(L.TAG_CCU_SYSTEM, "getAnalogOutValue- association: ${association.domainName}, analogOutAssociationType: $analogOutAssociationType")
                Pair (
                    getAnalogOutValueForLoopType(enable, analogOutAssociationType, ahuSettings,lockoutActiveDuringUnOccupied),
                    getLogicalOutput(analogOutAssociationType, enable, ahuSettings, lockoutActiveDuringUnOccupied)
                )

            }
            is ConnectModuleEquip -> {
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationTypeConnect.values()[association.readDefaultVal().toInt()]
                CcuLog.i(L.TAG_CCU_SYSTEM, "getAnalogOutValue- association: ${association.domainName}, analogOutAssociationType: $analogOutAssociationType")
                Pair (
                    getConnectAnalogOutValueForLoopType(enable, analogOutAssociationType, ahuSettings, lockoutActiveDuringUnOccupied),
                    getConnectLogicalOutput(analogOutAssociationType, enable, ahuSettings, lockoutActiveDuringUnOccupied)
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

    fun isLockoutActiveDuringUnoccupied(ahuSettings: AhuSettings): Boolean {
        return ((ahuSettings.isMechanicalCoolingAvailable && ahuSettings.systemState == State.COOLING) ||
                (ahuSettings.isMechanicalHeatingAvailable && ahuSettings.systemState == State.HEATING))
    }

}