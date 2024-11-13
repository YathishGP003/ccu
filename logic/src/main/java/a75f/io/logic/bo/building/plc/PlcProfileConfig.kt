package a75f.io.logic.bo.building.plc

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.PlcEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class PlcProfileConfig (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, val model : SeventyFiveFProfileDirective)
    : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var analog1InputType: ValueConfig
    lateinit var pidTargetValue: ValueConfig
    lateinit var thermistor1InputType: ValueConfig
    lateinit var pidProportionalRange: ValueConfig
    lateinit var nativeSensorType: ValueConfig

    lateinit var expectZeroErrorAtMidpoint: EnableConfig
    lateinit var invertControlLoopoutput: EnableConfig
    lateinit var useAnalogIn2ForSetpoint: EnableConfig

    lateinit var analog2InputType: ValueConfig
    lateinit var setpointSensorOffset: ValueConfig
    lateinit var analog1MinOutput: ValueConfig
    lateinit var analog1MaxOutput: ValueConfig

    lateinit var relay1OutputEnable: EnableConfig
    lateinit var relay2OutputEnable: EnableConfig
    lateinit var relay1OnThreshold: ValueConfig
    lateinit var relay2OnThreshold: ValueConfig
    lateinit var relay1OffThreshold: ValueConfig
    lateinit var relay2OffThreshold: ValueConfig

    fun getDefaultConfiguration() : PlcProfileConfig {
        analog1InputType = getDefaultValConfig(DomainName.analog1InputType, model)
        pidTargetValue = getDefaultValConfig(DomainName.pidTargetValue, model)
        thermistor1InputType = getDefaultValConfig(DomainName.thermistor1InputType, model)
        pidProportionalRange = getDefaultValConfig(DomainName.pidProportionalRange, model)
        nativeSensorType = getDefaultValConfig(DomainName.nativeSensorType, model)

        expectZeroErrorAtMidpoint = getDefaultEnableConfig(DomainName.expectZeroErrorAtMidpoint, model)
        invertControlLoopoutput = getDefaultEnableConfig(DomainName.invertControlLoopoutput, model)
        useAnalogIn2ForSetpoint = getDefaultEnableConfig(DomainName.useAnalogIn2ForSetpoint, model)

        analog2InputType = getDefaultValConfig(DomainName.analog2InputType, model)
        setpointSensorOffset = getDefaultValConfig(DomainName.setpointSensorOffset, model)
        analog1MinOutput = getDefaultValConfig(DomainName.analog1MinOutput, model)
        analog1MaxOutput = getDefaultValConfig(DomainName.analog1MaxOutput, model)

        relay1OutputEnable = getDefaultEnableConfig(DomainName.relay1OutputEnable, model)
        relay2OutputEnable = getDefaultEnableConfig(DomainName.relay2OutputEnable, model)
        relay1OnThreshold = getDefaultValConfig(DomainName.relay1OnThreshold, model)
        relay2OnThreshold = getDefaultValConfig(DomainName.relay2OnThreshold, model)
        relay1OffThreshold = getDefaultValConfig(DomainName.relay1OffThreshold, model)
        relay2OffThreshold = getDefaultValConfig(DomainName.relay2OffThreshold, model)
        isDefault = true
        return this
    }

    fun getActiveConfiguration() : PlcProfileConfig {

        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val plcEquip = PlcEquip(equip[Tags.ID].toString())

        getDefaultConfiguration()
        analog1InputType.currentVal = plcEquip.analog1InputType.readDefaultVal()
        pidTargetValue.currentVal = plcEquip.pidTargetValue.readDefaultVal()
        thermistor1InputType.currentVal = plcEquip.thermistor1InputType.readDefaultVal()
        pidProportionalRange.currentVal = plcEquip.pidProportionalRange.readDefaultVal()
        nativeSensorType.currentVal = plcEquip.nativeSensorType.readDefaultVal()

        expectZeroErrorAtMidpoint.enabled = plcEquip.expectZeroErrorAtMidpoint.readDefaultVal() > 0
        invertControlLoopoutput.enabled = plcEquip.invertControlLoopoutput.readDefaultVal() > 0
        useAnalogIn2ForSetpoint.enabled = plcEquip.useAnalogIn2ForSetpoint.readDefaultVal() > 0

        analog2InputType.currentVal = plcEquip.analog2InputType.readDefaultVal()
        setpointSensorOffset.currentVal = plcEquip.setpointSensorOffset.readDefaultVal()
        analog1MinOutput.currentVal = plcEquip.analog1MinOutput.readDefaultVal()
        analog1MaxOutput.currentVal = plcEquip.analog1MaxOutput.readDefaultVal()

        relay1OutputEnable.enabled = plcEquip.relay1OutputEnable.readDefaultVal() > 0
        relay2OutputEnable.enabled = plcEquip.relay2OutputEnable.readDefaultVal() > 0

        relay1OnThreshold.currentVal = plcEquip.relay1OnThreshold.readDefaultVal()
        relay2OnThreshold.currentVal = plcEquip.relay2OnThreshold.readDefaultVal()
        relay1OffThreshold.currentVal = plcEquip.relay1OffThreshold.readDefaultVal()
        relay2OffThreshold.currentVal = plcEquip.relay2OffThreshold.readDefaultVal()

        isDefault = false
        return this
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(expectZeroErrorAtMidpoint)
            add(invertControlLoopoutput)
            add(useAnalogIn2ForSetpoint)
            add(relay1OutputEnable)
            add(relay2OutputEnable)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
            add(analog1InputType)
            add(pidTargetValue)
            add(thermistor1InputType)
            add(pidProportionalRange)
            add(nativeSensorType)
            add(analog2InputType)
            add(setpointSensorOffset)
            add(analog1MinOutput)
            add(analog1MaxOutput)
            add(relay1OnThreshold)
            add(relay2OnThreshold)
            add(relay1OffThreshold)
            add(relay2OffThreshold)
        }
    }

    override fun toString(): String {
        return " analog1InputType ${analog1InputType.currentVal}  pidTargetValue ${pidTargetValue.currentVal} thermistor1InputType ${thermistor1InputType.currentVal}"+
            "pidProportionalRange ${pidProportionalRange.currentVal} nativeSensorType ${nativeSensorType.currentVal} expectZeroErrorAtMidpoint ${expectZeroErrorAtMidpoint.enabled}"+
                "invertControlLoopoutput ${invertControlLoopoutput.enabled} useAnalogIn2ForSetpoint ${useAnalogIn2ForSetpoint.enabled} analog2InputType ${analog2InputType.currentVal}" +
                "setpointSensorOffset ${setpointSensorOffset.currentVal} analog1MinOutput ${analog1MinOutput.currentVal} analog1MaxOutput ${analog1MaxOutput.currentVal}" +
                "relay1OutputEnable ${relay1OutputEnable.enabled} relay2OutputEnable ${relay2OutputEnable.enabled} relay1OnThreshold ${relay1OnThreshold.currentVal} relay2OnThreshold ${relay2OnThreshold.currentVal}" +
                "relay1OffThreshold ${relay1OffThreshold.currentVal} relay2OffThreshold ${relay2OffThreshold.currentVal}"
    }

}