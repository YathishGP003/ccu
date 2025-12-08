package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator

import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstatsplit.UnitVentilatorEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

abstract class UnitVentilatorConfiguration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String,
    profileType: ProfileType, model: SeventyFiveFProfileDirective
): HyperStatSplitConfiguration(
    nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model
) {
    lateinit var controlVia: ValueConfig
    lateinit var saTempering: EnableConfig
    abstract fun getActiveConfiguration(): HyperStatSplitConfiguration


    override fun getEnableConfigs(): List<EnableConfig> {
        return super.getEnableConfigs().toMutableList().apply {
            add(saTempering)
        }
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return super.getValueConfigs().toMutableList().apply {
            add(controlVia)
        }
    }

     fun getUvBasedConfig(uvEquip: UnitVentilatorEquip) {
        apply {
            controlVia.currentVal = uvEquip.controlVia.readDefaultVal()
            saTempering.enabled = uvEquip.enableSaTemperingControl.readDefaultVal() > 0.0
        }
    }

}