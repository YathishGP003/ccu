package a75f.io.logic.bo.building.bypassdamper

import a75f.io.domain.BypassDamperEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class BypassDamperProfileConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration (nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var damperType: ValueConfig
    lateinit var damperMinPosition: ValueConfig
    lateinit var damperMaxPosition: ValueConfig

    lateinit var pressureSensorType: ValueConfig
    lateinit var sensorMinVoltage: ValueConfig
    lateinit var sensorMaxVoltage: ValueConfig
    lateinit var pressureSensorMinVal: ValueConfig
    lateinit var pressureSensorMaxVal: ValueConfig

    lateinit var satMinThreshold: ValueConfig
    lateinit var satMaxThreshold: ValueConfig

    lateinit var pressureSetpoint: ValueConfig
    lateinit var expectedPressureError: ValueConfig

    fun getDefaultConfiguration() : BypassDamperProfileConfiguration {
        damperType = getDefaultValConfig(DomainName.damperType, model)
        damperMinPosition = getDefaultValConfig(DomainName.damperMinPosition, model)
        damperMaxPosition = getDefaultValConfig(DomainName.damperMaxPosition, model)

        pressureSensorType = getDefaultValConfig(DomainName.pressureSensorType, model)
        sensorMinVoltage = getDefaultValConfig(DomainName.sensorMinVoltage, model)
        sensorMaxVoltage = getDefaultValConfig(DomainName.sensorMaxVoltage, model)
        pressureSensorMinVal = getDefaultValConfig(DomainName.pressureSensorMinVal, model)
        pressureSensorMaxVal = getDefaultValConfig(DomainName.pressureSensorMaxVal, model)

        satMinThreshold = getDefaultValConfig(DomainName.satMinThreshold, model)
        satMaxThreshold = getDefaultValConfig(DomainName.satMaxThreshold, model)

        pressureSetpoint = getDefaultValConfig(DomainName.ductStaticPressureSetpoint, model)
        expectedPressureError = getDefaultValConfig(DomainName.expectedPressureError, model)

        isDefault = true

        return this
    }

    fun getActiveConfiguration() : BypassDamperProfileConfiguration {

        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val bdEquip = BypassDamperEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        damperType.currentVal = bdEquip.damperType.readDefaultVal()
        damperMinPosition.currentVal = bdEquip.damperMinPosition.readDefaultVal()
        damperMaxPosition.currentVal = bdEquip.damperMaxPosition.readDefaultVal()

        pressureSensorType.currentVal = bdEquip.pressureSensorType.readDefaultVal()

        satMinThreshold.currentVal = bdEquip.satMinThreshold.readDefaultVal()
        satMaxThreshold.currentVal = bdEquip.satMaxThreshold.readDefaultVal()

        pressureSetpoint.currentVal = bdEquip.ductStaticPressureSetpoint.readDefaultVal()
        expectedPressureError.currentVal = bdEquip.expectedPressureError.readDefaultVal()

        // handle pressure sensor type
        if (pressureSensorType.currentVal > 0.0) {
            pressureSensorMinVal.currentVal = bdEquip.pressureSensorMinVal.readDefaultVal()
            pressureSensorMaxVal.currentVal = bdEquip.pressureSensorMaxVal.readDefaultVal()
            sensorMinVoltage.currentVal = bdEquip.sensorMinVoltageOutput.readDefaultVal()
            sensorMaxVoltage.currentVal = bdEquip.sensorMaxVoltageOutput.readDefaultVal()
        } else {
            pressureSensorMinVal = getDefaultValConfig(DomainName.pressureSensorMinVal, model)
            pressureSensorMaxVal = getDefaultValConfig(DomainName.pressureSensorMaxVal, model)
            sensorMinVoltage = getDefaultValConfig(DomainName.sensorMinVoltage, model)
            sensorMaxVoltage = getDefaultValConfig(DomainName.sensorMaxVoltage, model)
        }

        isDefault = false
        return this
    }

    override fun getAssociationConfigs() : List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(damperType)
            add(damperMinPosition)
            add(damperMaxPosition)
            add(pressureSensorType)
            add(pressureSensorMinVal)
            add(pressureSensorMaxVal)
            add(sensorMinVoltage)
            add(sensorMaxVoltage)
            add(satMinThreshold)
            add(satMaxThreshold)
            add(pressureSetpoint)
            add(expectedPressureError)
        }
    }
    override fun toString(): String {
        return " damperType ${damperType.currentVal} damperMinPosition ${damperMinPosition.currentVal} damperMaxPosition ${damperMaxPosition.currentVal}"+
            "pressureSensorType ${pressureSensorType.currentVal} pressureSensorMinVal ${pressureSensorMinVal.currentVal} pressureSensorMaxVal ${pressureSensorMaxVal.currentVal}"+
                "sensorMinVoltage ${sensorMinVoltage.currentVal} sensorMaxVoltage ${sensorMaxVoltage.currentVal} satMinThreshold ${satMinThreshold.currentVal}"+
                " satMaxThreshold ${satMaxThreshold.currentVal} ductStaticPressureSetpoint ${pressureSetpoint.currentVal} expectedPressureError ${expectedPressureError.currentVal}"
    }
}