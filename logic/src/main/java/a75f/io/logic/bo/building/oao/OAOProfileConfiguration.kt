package a75f.io.logic.bo.building.oao

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.domain.OAOEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.logic.bo.building.dab.getDevicePointDict
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
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

    fun updateDevicePoints(
        hayStack: CCUHsApi,
        config: OAOProfileConfiguration,
        deviceBuilder: DeviceBuilder,
        deviceModel: SeventyFiveFDeviceDirective,
        isReconfig : Boolean = false
    ) {
        val deviceEntityId =
            hayStack.readEntity("device and addr == \"${config.nodeAddress}\"")["id"].toString()
        val device = Device.Builder().setHDict(hayStack.readHDictById(deviceEntityId)).build()

        fun updateDevicePoint(domainName: String, port: String, analogType: Any) {
            val pointDef = deviceModel.points.find { it.domainName == domainName }
            pointDef?.let {
                val pointDict = getDevicePointDict(domainName, deviceEntityId, hayStack).apply {
                    this["port"] = port
                    this["analogType"] = analogType
                }
                deviceBuilder.updatePoint(it, config, device, pointDict)
            }
        }

        //Update analog input points
        updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 5)
        updateDevicePoint(
            DomainName.analog2In,
            Port.ANALOG_IN_TWO.name,
            8 + config.currentTransformerType.currentVal
        )

        //Update analog output points
        updateDevicePoint(
            DomainName.analog1Out,
            Port.ANALOG_OUT_ONE.name,
            "${config.outsideDamperMinDrive.currentVal} - ${config.outsideDamperMaxDrive.currentVal}"
        )
        updateDevicePoint(
            DomainName.analog2Out,
            Port.ANALOG_OUT_TWO.name,
            "${config.returnDamperMinDrive.currentVal} - ${config.returnDamperMaxDrive.currentVal}"
        )

        // not updating below points if reconfiguring
        if(isReconfig)
            return

        //Update TH input points
        updateDevicePoint(DomainName.th1In, Port.TH1_IN.name, 0)
        updateDevicePoint(DomainName.th2In, Port.TH2_IN.name, 0)

        //Update relay points
        updateDevicePoint(
            DomainName.relay1,
            Port.RELAY_ONE.name,
            OutputRelayActuatorType.NormallyClose.displayName
        )
        updateDevicePoint(
            DomainName.relay2,
            Port.RELAY_TWO.name,
            OutputRelayActuatorType.NormallyClose.displayName
        )

    }
}