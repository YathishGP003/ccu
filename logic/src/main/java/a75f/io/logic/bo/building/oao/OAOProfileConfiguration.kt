package a75f.io.logic.bo.building.oao

import a75f.io.domain.OAOEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class OAOProfileConfiguration(
    nodeAddress: Int,
    nodeType: String,
    priority: Int,
    roomRef: String,
    floorRef: String,
    profileType: ProfileType,
    val model: SeventyFiveFProfileDirective
) : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    lateinit var outsideDamperMinDrive: ValueConfig
    lateinit var outsideDamperMaxDrive: ValueConfig
    lateinit var returnDamperMinDrive: ValueConfig
    lateinit var returnDamperMaxDrive: ValueConfig
    lateinit var outsideDamperMinOpenDuringRecirculation: ValueConfig
    lateinit var outsideDamperMinOpenDuringConditioning: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanLow: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanMedium: ValueConfig
    lateinit var outsideDamperMinOpenDuringFanHigh: ValueConfig
    lateinit var returnDamperMinOpen: ValueConfig
    lateinit var exhaustFanStage1Threshold: ValueConfig
    lateinit var exhaustFanStage2Threshold: ValueConfig
    lateinit var currentTransformerType: ValueConfig
    lateinit var co2Threshold: ValueConfig
    lateinit var exhaustFanHysteresis: ValueConfig
    lateinit var usePerRoomCO2Sensing: EnableConfig
    lateinit var systemPurgeOutsideDamperMinPos: ValueConfig
    lateinit var enhancedVentilationOutsideDamperMinOpen: ValueConfig

    fun getDefaultConfiguration(): OAOProfileConfiguration {
        outsideDamperMinDrive = getDefaultValConfig(DomainName.outsideDamperMinDrive, model)
        outsideDamperMaxDrive = getDefaultValConfig(DomainName.outsideDamperMaxDrive, model)
        returnDamperMinDrive = getDefaultValConfig(DomainName.returnDamperMinDrive, model)
        returnDamperMaxDrive = getDefaultValConfig(DomainName.returnDamperMaxDrive, model)
        outsideDamperMinOpenDuringRecirculation = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringRecirculation, model)
        outsideDamperMinOpenDuringConditioning = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringConditioning, model)
        outsideDamperMinOpenDuringFanLow = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanLow, model)
        outsideDamperMinOpenDuringFanMedium = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanMedium, model)
        outsideDamperMinOpenDuringFanHigh = getDefaultValConfig(DomainName.outsideDamperMinOpenDuringFanHigh, model)
        returnDamperMinOpen = getDefaultValConfig(DomainName.returnDamperMinOpen, model)
        exhaustFanStage1Threshold = getDefaultValConfig(DomainName.exhaustFanStage1Threshold, model)
        exhaustFanStage2Threshold = getDefaultValConfig(DomainName.exhaustFanStage2Threshold, model)
        currentTransformerType = getDefaultValConfig(DomainName.currentTransformerType, model)
        co2Threshold = getDefaultValConfig(DomainName.co2Threshold, model)
        exhaustFanHysteresis = getDefaultValConfig(DomainName.exhaustFanHysteresis, model)
        usePerRoomCO2Sensing = getDefaultEnableConfig(DomainName.usePerRoomCO2Sensing, model)
        systemPurgeOutsideDamperMinPos = getDefaultValConfig(DomainName.systemPurgeOutsideDamperMinPos, model)
        enhancedVentilationOutsideDamperMinOpen = getDefaultValConfig(DomainName.enhancedVentilationOutsideDamperMinOpen, model)
        isDefault = true
        return this
    }

    fun getActiveConfiguration(): OAOProfileConfiguration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val oaoEquip = OAOEquip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        outsideDamperMinDrive.currentVal = oaoEquip.outsideDamperMinDrive.readDefaultVal()
        outsideDamperMaxDrive.currentVal = oaoEquip.outsideDamperMaxDrive.readDefaultVal()
        returnDamperMinDrive.currentVal = oaoEquip.returnDamperMinDrive.readDefaultVal()
        returnDamperMaxDrive.currentVal = oaoEquip.returnDamperMaxDrive.readDefaultVal()
        outsideDamperMinOpenDuringRecirculation.currentVal =
            oaoEquip.outsideDamperMinOpenDuringRecirculation.readDefaultVal()
        outsideDamperMinOpenDuringConditioning.currentVal =
            oaoEquip.outsideDamperMinOpenDuringConditioning.readDefaultVal()
        outsideDamperMinOpenDuringFanLow.currentVal =
            oaoEquip.outsideDamperMinOpenDuringFanLow.readDefaultVal()
        outsideDamperMinOpenDuringFanMedium.currentVal =
            oaoEquip.outsideDamperMinOpenDuringFanMedium.readDefaultVal()
        outsideDamperMinOpenDuringFanHigh.currentVal =
            oaoEquip.outsideDamperMinOpenDuringFanHigh.readDefaultVal()
        returnDamperMinOpen.currentVal = oaoEquip.returnDamperMinOpen.readDefaultVal()
        exhaustFanStage1Threshold.currentVal = oaoEquip.exhaustFanStage1Threshold.readDefaultVal()
        exhaustFanStage2Threshold.currentVal = oaoEquip.exhaustFanStage2Threshold.readDefaultVal()
        currentTransformerType.currentVal = oaoEquip.currentTransformerType.readDefaultVal()
        co2Threshold.currentVal = oaoEquip.co2Threshold.readDefaultVal()
        exhaustFanHysteresis.currentVal = oaoEquip.exhaustFanHysteresis.readDefaultVal()
        usePerRoomCO2Sensing.enabled = oaoEquip.usePerRoomCO2Sensing.readDefaultVal() > 0
        systemPurgeOutsideDamperMinPos.currentVal =
            oaoEquip.systemPurgeOutsideDamperMinPos.readDefaultVal()
        enhancedVentilationOutsideDamperMinOpen.currentVal =
            oaoEquip.enhancedVentilationOutsideDamperMinOpen.readDefaultVal()
        isDefault = false
        return this
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return "outsideDamperMinDrive: ${outsideDamperMinDrive.currentVal},"+
                "outsideDamperMaxDrive: ${outsideDamperMaxDrive.currentVal},"+
                "returnDamperMinDrive: ${returnDamperMinDrive.currentVal},"+
                "returnDamperMaxDrive: ${returnDamperMaxDrive.currentVal},"+
                "outsideDamperMinOpenDuringRecirculation: ${outsideDamperMinOpenDuringRecirculation.currentVal},"+
                "outsideDamperMinOpenDuringConditioning: ${outsideDamperMinOpenDuringConditioning.currentVal},"+
                "outsideDamperMinOpenDuringFanLow: ${outsideDamperMinOpenDuringFanLow.currentVal},"+
                "outsideDamperMinOpenDuringFanMedium: ${outsideDamperMinOpenDuringFanMedium.currentVal},"+
                "outsideDamperMinOpenDuringFanHigh: ${outsideDamperMinOpenDuringFanHigh.currentVal},"+
                "returnDamperMinOpen: ${returnDamperMinOpen.currentVal},"+
                "exhaustFanStage1Threshold: ${exhaustFanStage1Threshold.currentVal},"+
                "exhaustFanStage2Threshold: ${exhaustFanStage2Threshold.currentVal},"+
                "currentTransformerType: ${currentTransformerType.currentVal},"+
                "co2Threshold: ${co2Threshold.currentVal},"+
                "exhaustFanHysteresis: ${exhaustFanHysteresis.currentVal},"+
                "usePerRoomCO2Sensing: ${usePerRoomCO2Sensing.enabled},"+
                "systemPurgeOutsideDamperMinPos: ${systemPurgeOutsideDamperMinPos.currentVal},"+
                "enhancedVentilationOutsideDamperMinOpen: ${enhancedVentilationOutsideDamperMinOpen.currentVal},"
    }

    override fun getValueConfigs(): List<ValueConfig> {
        return mutableListOf<ValueConfig>().apply {
                add(outsideDamperMinDrive)
                add(outsideDamperMaxDrive)
                add(returnDamperMinDrive)
                add(returnDamperMaxDrive)
                add(outsideDamperMinOpenDuringRecirculation)
                add(outsideDamperMinOpenDuringConditioning)
                add(outsideDamperMinOpenDuringFanLow)
                add(outsideDamperMinOpenDuringFanMedium)
                add(outsideDamperMinOpenDuringFanHigh)
                add(returnDamperMinOpen)
                add(exhaustFanStage1Threshold)
                add(exhaustFanStage2Threshold)
                add(currentTransformerType)
                add(co2Threshold)
                add(exhaustFanHysteresis)
                add(systemPurgeOutsideDamperMinPos)
                add(enhancedVentilationOutsideDamperMinOpen)
        }
    }

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
            add(usePerRoomCO2Sensing)
        }
    }
}