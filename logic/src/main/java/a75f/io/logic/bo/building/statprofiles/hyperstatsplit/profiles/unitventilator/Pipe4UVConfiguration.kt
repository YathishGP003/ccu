package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.equips.hyperstatsplit.Pipe4UVEquip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags
import java.util.Collections

class Pipe4UVConfiguration(
    nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String,
    profileType: ProfileType, model: SeventyFiveFProfileDirective
) : UnitVentilatorConfiguration(
    nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model
) {
    lateinit var analogOut1Voltage: AnalogOutVoltage
    lateinit var analogOut2Voltage: AnalogOutVoltage
    lateinit var analogOut3Voltage: AnalogOutVoltage
    lateinit var analogOut4Voltage: AnalogOutVoltage

    override fun getValueConfigs(): List<ValueConfig> {
        return super.getValueConfigs().toMutableList().apply {
            Collections.addAll(addValueConfig(analogOut1Voltage, this))
            Collections.addAll(addValueConfig(analogOut2Voltage, this))
            Collections.addAll(addValueConfig(analogOut3Voltage, this))
            Collections.addAll(addValueConfig(analogOut4Voltage, this))
        }
    }

    override fun isCoolingAvailable(): Boolean {
        return (isAnyRelayEnabledAndMapped(this, Pipe4UVRelayControls.COOLING_WATER_VALVE.name)
                || isAnyAnalogEnabledAndMapped(this, Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.name
        ))
    }
    override fun isHeatingAvailable(): Boolean {
        return (isAnyRelayEnabledAndMapped(this, Pipe4UVRelayControls.HEATING_WATER_VALVE.name) || isAnyAuxHeatingStageMapped()
                || isAnyAnalogEnabledAndMapped(this, Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.name))
    }

    override fun getDefaultConfiguration(): UnitVentilatorConfiguration {
        var config = super.getDefaultConfiguration()
        config = config as Pipe4UVConfiguration
        config.apply {
            config.getAnalogOut1Voltage()
            config.getAnalogOut2Voltage()
            config.getAnalogOut3Voltage()
            config.getAnalogOut4Voltage()
            config.controlVia = getDefaultValConfig(DomainName.controlVia, model)
            config.saTempering = getDefaultEnableConfig(DomainName.enableSaTemperingControl, model)
        }
        return config
    }

    override fun getActiveConfiguration(): Pipe4UVConfiguration {
        val equip = Domain.hayStack.readEntity("equip and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            return this
        }
        val pipe4Equip = Pipe4UVEquip(equip[Tags.ID].toString())
        getDefaultConfiguration()
        getActiveEnableConfigs(pipe4Equip)
        getActiveAssociationConfigs(pipe4Equip)
        getGenericZoneConfigs(pipe4Equip)
        getAnalogOutConfigs(pipe4Equip)
        getUvBasedConfig(pipe4Equip)
        equipId = pipe4Equip.equipRef
        isDefault = false
        return this
    }

    override fun getRelayMap(): Map<String, Boolean> {
        val relays = mutableMapOf<String, Boolean>()
        relays[DomainName.relay1] = isRelayExternalMapped(relay1Enabled,relay1Association)
        relays[DomainName.relay2] = isRelayExternalMapped(relay2Enabled,relay2Association)
        relays[DomainName.relay3] = isRelayExternalMapped(relay3Enabled,relay3Association)
        relays[DomainName.relay4] = isRelayExternalMapped(relay4Enabled,relay4Association)
        relays[DomainName.relay5] = isRelayExternalMapped(relay5Enabled,relay5Association)
        relays[DomainName.relay6] = isRelayExternalMapped(relay6Enabled,relay6Association)
        relays[DomainName.relay7] = isRelayExternalMapped(relay7Enabled,relay7Association)
        relays[DomainName.relay8] = isRelayExternalMapped(relay8Enabled,relay8Association)
        return relays
    }

    override fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
       val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        analogOuts[DomainName.analog1Out] = Pair(isAnalogOutExternallyMapped(analogOut1Enabled,analogOut1Association), analogType(analogOut1Enabled))
        analogOuts[DomainName.analog2Out] = Pair(isAnalogOutExternallyMapped(analogOut2Enabled,analogOut2Association), analogType(analogOut2Enabled))
        analogOuts[DomainName.analog3Out] = Pair(isAnalogOutExternallyMapped(analogOut3Enabled,analogOut3Association), analogType(analogOut3Enabled))
        analogOuts[DomainName.analog4Out] = Pair(isAnalogOutExternallyMapped(analogOut4Enabled,analogOut4Association), analogType(analogOut4Enabled))
        return analogOuts
    }

    private fun analogType(analogOutPort : EnableConfig):String{
        return when(analogOutPort) {
            analogOut1Enabled ->getPortType(analogOut1Association, analogOut1Voltage)
            analogOut2Enabled -> getPortType(analogOut2Association, analogOut2Voltage)
            analogOut3Enabled -> getPortType(analogOut3Association, analogOut3Voltage)
            analogOut4Enabled -> getPortType(analogOut4Association, analogOut4Voltage)
            else -> "2-10v"
        }
    }
    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) =
        (enabled.enabled && association.associationVal == Pipe4UVRelayControls.EXTERNALLY_MAPPED.ordinal)

    private  fun isAnalogOutExternallyMapped(enabled: EnableConfig,association: AssociationConfig) =
        (enabled.enabled && association.associationVal == Pipe4UvAnalogOutControls.EXTERNALLY_MAPPED.ordinal)

    private fun getPortType(
        association: AssociationConfig,
        minMaxConfig: AnalogOutVoltage
    ): String {
        val portType = when(association.associationVal){
            Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal ->{
                "${minMaxConfig.hotWaterValveMinVoltage.currentVal.toInt()}-${minMaxConfig.hotWaterValveMaxVoltage.currentVal.toInt()}v"
            }
            Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal->{
                "${minMaxConfig.coolingWaterValveMinVoltage.currentVal.toInt()}-${minMaxConfig.coolingWaterValveMaxVoltage.currentVal.toInt()}v"
            }
            Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal ->{
                "${minMaxConfig.faceAndBypassDamperMin.currentVal.toInt()}-${minMaxConfig.faceAndBypassDamperMax.currentVal.toInt()}v"
            }
            Pipe4UvAnalogOutControls.FAN_SPEED.ordinal -> {
                "${minMaxConfig.fanMin.currentVal.toInt()}-${minMaxConfig.fanMax.currentVal.toInt()}v"
            }
            Pipe4UvAnalogOutControls.OAO_DAMPER.ordinal -> {
                "${minMaxConfig.oaoDamperMinVoltage.currentVal.toInt()}-${minMaxConfig.oaoDamperMaxVoltage.currentVal.toInt()}v"

            }
            Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal -> {
                "${minMaxConfig.dcvModulationMinVoltage.currentVal.toInt()}-${minMaxConfig.oaoDamperMaxVoltage.currentVal.toInt()}v"
            }

            else -> {
                 "2-10v"
            }
        }
        return portType
    }


    private fun getAnalogOutConfigs(equip: Pipe4UVEquip) {
        analogOut1Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog1MinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog1MaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog1MinDCVDamper, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog1MaxDCVDamper, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog1MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog1MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog1MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog1MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            hotWaterValveMinVoltage.currentVal =
                getDefault(equip.analog1MinHotWaterValve, equip, hotWaterValveMinVoltage)
            hotWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog1MaxHotWaterValve, equip, hotWaterValveMaxVoltage)
            coolingWaterValveMinVoltage.currentVal =
                getDefault(equip.analog1MinChilledWaterValve, equip, coolingWaterValveMinVoltage)
            coolingWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog1MaxChilledWaterValve, equip, coolingWaterValveMaxVoltage)
            fanAtLow.currentVal = getDefault(equip.analog1FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog1FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog1FanHigh, equip, fanAtHigh)
        }

        analogOut2Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog2MinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog2MaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog2MinDCVDamper, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog2MaxDCVDamper, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog2MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog2MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog2MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog2MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            hotWaterValveMinVoltage.currentVal =
                getDefault(equip.analog2MinHotWaterValve, equip, hotWaterValveMinVoltage)
            hotWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog2MaxHotWaterValve, equip, hotWaterValveMaxVoltage)
            coolingWaterValveMinVoltage.currentVal =
                getDefault(equip.analog2MinChilledWaterValve, equip, coolingWaterValveMinVoltage)
            coolingWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog2MaxChilledWaterValve, equip, coolingWaterValveMaxVoltage)
            fanAtLow.currentVal = getDefault(equip.analog2FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog2FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog2FanHigh, equip, fanAtHigh)
        }

        analogOut3Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog3MinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog3MaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog3MinDCVDamper, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog3MaxDCVDamper, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog3MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog3MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog3MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog3MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            hotWaterValveMinVoltage.currentVal =
                getDefault(equip.analog3MinHotWaterValve, equip, hotWaterValveMinVoltage)
            hotWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog3MaxHotWaterValve, equip, hotWaterValveMaxVoltage)
            coolingWaterValveMinVoltage.currentVal =
                getDefault(equip.analog3MinChilledWaterValve, equip, coolingWaterValveMinVoltage)
            coolingWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog3MaxChilledWaterValve, equip, coolingWaterValveMaxVoltage)
            fanAtLow.currentVal = getDefault(equip.analog3FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog3FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog3FanHigh, equip, fanAtHigh)
        }

        analogOut4Voltage.apply {
            oaoDamperMinVoltage.currentVal =
                getDefault(equip.analog4MinOAODamper, equip, oaoDamperMinVoltage)
            oaoDamperMaxVoltage.currentVal =
                getDefault(equip.analog4MaxOAODamper, equip, oaoDamperMaxVoltage)
            dcvModulationMinVoltage.currentVal =
                getDefault(equip.analog4MinDCVDamper, equip, dcvModulationMinVoltage)
            dcvModulationMaxVoltage.currentVal =
                getDefault(equip.analog4MaxDCVDamper, equip, dcvModulationMaxVoltage)
            fanMin.currentVal = getDefault(equip.analog4MinFanSpeed, equip, fanMin)
            fanMax.currentVal = getDefault(equip.analog4MaxFanSpeed, equip, fanMax)
            faceAndBypassDamperMin.currentVal =
                getDefault(equip.analog4MinFaceBypassDamper, equip, faceAndBypassDamperMin)
            faceAndBypassDamperMax.currentVal =
                getDefault(equip.analog4MaxFaceBypassDamper, equip, faceAndBypassDamperMax)
            hotWaterValveMinVoltage.currentVal =
                getDefault(equip.analog4MinHotWaterValve, equip, hotWaterValveMinVoltage)
            hotWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog4MaxHotWaterValve, equip, hotWaterValveMaxVoltage)
            coolingWaterValveMinVoltage.currentVal =
                getDefault(equip.analog4MinChilledWaterValve, equip, coolingWaterValveMinVoltage)
            coolingWaterValveMaxVoltage.currentVal =
                getDefault(equip.analog4MaxChilledWaterValve, equip, coolingWaterValveMaxVoltage)
            fanAtLow.currentVal = getDefault(equip.analog4FanLow, equip, fanAtLow)
            fanAtMedium.currentVal = getDefault(equip.analog4FanMedium, equip, fanAtMedium)
            fanAtHigh.currentVal = getDefault(equip.analog4FanHigh, equip, fanAtHigh)
        }
        outsideDamperMinOpenDuringRecirc.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringRecirculation,
            equip,
            outsideDamperMinOpenDuringRecirc
        )
        outsideDamperMinOpenDuringConditioning.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringConditioning,
            equip,
            outsideDamperMinOpenDuringConditioning
        )
        outsideDamperMinOpenDuringFanLow.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringFanLow,
            equip,
            outsideDamperMinOpenDuringFanLow
        )
        outsideDamperMinOpenDuringFanMedium.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringFanMedium,
            equip,
            outsideDamperMinOpenDuringFanMedium
        )
        outsideDamperMinOpenDuringFanHigh.currentVal = getDefault(
            equip.outsideDamperMinOpenDuringFanHigh,
            equip,
            outsideDamperMinOpenDuringFanHigh
        )
        zoneCO2DamperOpeningRate.currentVal =
            getDefault(equip.co2DamperOpeningRate, equip, zoneCO2DamperOpeningRate)

    }

    private fun getAnalogOut1Voltage() {
        analogOut1Voltage = AnalogOutVoltage(
            fanMin = getDefaultValConfig(DomainName.analog1MinFanSpeed, model),
            fanMax = getDefaultValConfig(DomainName.analog1MaxFanSpeed, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog1MaxOAODamper, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog1MinOAODamper, model),
            faceAndBypassDamperMax = getDefaultValConfig(DomainName.analog1MaxFaceBypassDamper, model),
            faceAndBypassDamperMin = getDefaultValConfig(DomainName.analog1MinFaceBypassDamper, model),
            hotWaterValveMinVoltage = getDefaultValConfig(DomainName.analog1MinHotWaterValve, model),
            hotWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog1MaxHotWaterValve, model),
            coolingWaterValveMinVoltage = getDefaultValConfig(DomainName.analog1MinChilledWaterValve, model),
            coolingWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog1MaxChilledWaterValve, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog1MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog1MaxDCVDamper, model),
            fanAtLow = getDefaultValConfig(DomainName.analog1FanLow, model),
            fanAtMedium = getDefaultValConfig(DomainName.analog1FanMedium, model),
            fanAtHigh = getDefaultValConfig(DomainName.analog1FanHigh, model)
        )
    }

    private fun getAnalogOut2Voltage() {
        analogOut2Voltage = AnalogOutVoltage(
            fanMin = getDefaultValConfig(DomainName.analog2MinFanSpeed, model),
            fanMax = getDefaultValConfig(DomainName.analog2MaxFanSpeed, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog2MaxOAODamper, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog2MinOAODamper, model),
            faceAndBypassDamperMax = getDefaultValConfig(DomainName.analog2MaxFaceBypassDamper, model),
            faceAndBypassDamperMin = getDefaultValConfig(DomainName.analog2MinFaceBypassDamper, model),
            hotWaterValveMinVoltage = getDefaultValConfig(DomainName.analog2MinHotWaterValve, model),
            hotWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog2MaxHotWaterValve, model),
            coolingWaterValveMinVoltage = getDefaultValConfig(DomainName.analog2MinChilledWaterValve, model),
            coolingWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog2MaxChilledWaterValve, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog2MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog2MaxDCVDamper, model),
            fanAtLow = getDefaultValConfig(DomainName.analog2FanLow, model),
            fanAtMedium = getDefaultValConfig(DomainName.analog2FanMedium, model),
            fanAtHigh = getDefaultValConfig(DomainName.analog2FanHigh, model)
        )
    }

    private fun getAnalogOut3Voltage() {
        analogOut3Voltage = AnalogOutVoltage(
            fanMin = getDefaultValConfig(DomainName.analog3MinFanSpeed, model),
            fanMax = getDefaultValConfig(DomainName.analog3MaxFanSpeed, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog3MaxOAODamper, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog3MinOAODamper, model),
            faceAndBypassDamperMax = getDefaultValConfig(DomainName.analog3MaxFaceBypassDamper, model),
            faceAndBypassDamperMin = getDefaultValConfig(DomainName.analog3MinFaceBypassDamper, model),
            hotWaterValveMinVoltage = getDefaultValConfig(DomainName.analog3MinHotWaterValve, model),
            hotWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog3MaxHotWaterValve, model),
            coolingWaterValveMinVoltage = getDefaultValConfig(DomainName.analog3MinChilledWaterValve, model),
            coolingWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog3MaxChilledWaterValve, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog3MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog3MaxDCVDamper, model),
            fanAtLow = getDefaultValConfig(DomainName.analog3FanLow, model),
            fanAtMedium = getDefaultValConfig(DomainName.analog3FanMedium, model),
            fanAtHigh = getDefaultValConfig(DomainName.analog3FanHigh, model)
        )

    }

    private fun getAnalogOut4Voltage() {
        analogOut4Voltage = AnalogOutVoltage(
            fanMin = getDefaultValConfig(DomainName.analog4MinFanSpeed, model),
            fanMax = getDefaultValConfig(DomainName.analog4MaxFanSpeed, model),
            oaoDamperMaxVoltage = getDefaultValConfig(DomainName.analog4MaxOAODamper, model),
            oaoDamperMinVoltage = getDefaultValConfig(DomainName.analog4MinOAODamper, model),
            faceAndBypassDamperMax = getDefaultValConfig(DomainName.analog4MaxFaceBypassDamper, model),
            faceAndBypassDamperMin = getDefaultValConfig(DomainName.analog4MinFaceBypassDamper, model),
            hotWaterValveMinVoltage = getDefaultValConfig(DomainName.analog4MinHotWaterValve, model),
            hotWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog4MaxHotWaterValve, model),
            coolingWaterValveMinVoltage = getDefaultValConfig(DomainName.analog4MinChilledWaterValve, model),
            coolingWaterValveMaxVoltage = getDefaultValConfig(DomainName.analog4MaxChilledWaterValve, model),
            dcvModulationMinVoltage = getDefaultValConfig(DomainName.analog4MinDCVDamper, model),
            dcvModulationMaxVoltage = getDefaultValConfig(DomainName.analog4MaxDCVDamper, model),
            fanAtLow = getDefaultValConfig(DomainName.analog4FanLow, model),
            fanAtMedium = getDefaultValConfig(DomainName.analog4FanMedium, model),
            fanAtHigh = getDefaultValConfig(DomainName.analog4FanHigh, model)
        )
    }

    /**
     * Add all value configs to the list
     */
    private fun addValueConfig(
        analogConfig: AnalogOutVoltage, list: MutableList<ValueConfig>
    ): MutableList<ValueConfig> {
        return list.apply {
            add(analogConfig.oaoDamperMinVoltage)
            add(analogConfig.oaoDamperMaxVoltage)
            add(analogConfig.hotWaterValveMaxVoltage)
            add(analogConfig.hotWaterValveMinVoltage)
            add(analogConfig.coolingWaterValveMinVoltage)
            add(analogConfig.coolingWaterValveMaxVoltage)
            add(analogConfig.fanMin)
            add(analogConfig.fanMax)
            add(analogConfig.faceAndBypassDamperMin)
            add(analogConfig.faceAndBypassDamperMax)
            add(analogConfig.fanAtLow)
            add(analogConfig.fanAtMedium)
            add(analogConfig.fanAtHigh)
            add(analogConfig.dcvModulationMinVoltage)
            add(analogConfig.dcvModulationMaxVoltage)
        }
    }

    fun getFanConfiguration(port: Port): AnalogOutVoltage {
        return when (port) {
            Port.ANALOG_OUT_ONE -> analogOut1Voltage
            Port.ANALOG_OUT_TWO -> analogOut2Voltage
            Port.ANALOG_OUT_THREE -> analogOut3Voltage
            Port.ANALOG_OUT_FOUR -> analogOut4Voltage
            else -> analogOut1Voltage
        }
    }

    fun getLowestFanSelected(): Int {
        val lowestSelected = if (isAnyRelayMappedToFanLowVentilation()) {
            getLowestStage(
                Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION.ordinal,
                Pipe4UVRelayControls.FAN_MEDIUM_SPEED.ordinal,
                Pipe4UVRelayControls.FAN_HIGH_SPEED.ordinal
            )
        } else {
            getLowestStage(
                Pipe4UVRelayControls.FAN_LOW_SPEED.ordinal,
                Pipe4UVRelayControls.FAN_MEDIUM_SPEED.ordinal,
                Pipe4UVRelayControls.FAN_HIGH_SPEED.ordinal,
            )
        }
        if (lowestSelected == -1) return -1
        return Pipe4UVRelayControls.values()[lowestSelected].ordinal
    }

    override fun getHighestFanStageCount(): Int {
        val stageOrdinal = getHighestStage(
            if (isAnyRelayMappedToFanLowVentilation()) {
                Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION.ordinal
            } else {
                Pipe4UVRelayControls.FAN_LOW_SPEED.ordinal
            },
            Pipe4UVRelayControls.FAN_MEDIUM_SPEED.ordinal,
            Pipe4UVRelayControls.FAN_HIGH_SPEED.ordinal
        )
        return when (stageOrdinal) {
            Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION.ordinal -> 1
            Pipe4UVRelayControls.FAN_LOW_SPEED.ordinal -> 1
            Pipe4UVRelayControls.FAN_MEDIUM_SPEED.ordinal -> 2
            Pipe4UVRelayControls.FAN_HIGH_SPEED.ordinal -> 3
            else -> 0
        }
    }

    private fun isAnyRelayMappedToFanLowVentilation(): Boolean {
        return isAnyRelayEnabledAndMapped(this,Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION.name)
    }

    private fun isAnyAuxHeatingStageMapped(): Boolean {
        return isAnyRelayEnabledAndMapped(this,Pipe4UVRelayControls.AUX_HEATING_STAGE1.name)
                || isAnyRelayEnabledAndMapped(this,Pipe4UVRelayControls.AUX_HEATING_STAGE2.name)
    }

    data class AnalogOutVoltage(
        var oaoDamperMinVoltage: ValueConfig, var oaoDamperMaxVoltage: ValueConfig,
        var fanMin: ValueConfig, var fanMax: ValueConfig,
        var fanAtLow: ValueConfig, var fanAtMedium: ValueConfig, var fanAtHigh: ValueConfig,
        var hotWaterValveMinVoltage: ValueConfig, var hotWaterValveMaxVoltage: ValueConfig,
        var coolingWaterValveMinVoltage: ValueConfig, var coolingWaterValveMaxVoltage: ValueConfig,
        var dcvModulationMinVoltage: ValueConfig, var dcvModulationMaxVoltage: ValueConfig,
        var faceAndBypassDamperMin: ValueConfig, var faceAndBypassDamperMax: ValueConfig
    )

    override fun toString(): String {
        return super.toString().apply {
            "satTempering=$saTempering, controlVia=$controlVia, analogOut1Voltage=$analogOut1Voltage, analogOut2Voltage=$analogOut2Voltage, analogOut3Voltage=$analogOut3Voltage, analogOut4Voltage=$analogOut4Voltage"
        }
    }
}

enum class Pipe4UvAnalogOutControls {
    HEATING_WATER_MODULATING_VALVE, COOLING_WATER_MODULATING_VALVE, FACE_DAMPER_VALVE, FAN_SPEED, OAO_DAMPER, DCV_MODULATING_DAMPER, EXTERNALLY_MAPPED
}

enum class Pipe4UVRelayControls {
    FAN_LOW_SPEED_VENTILATION,
    FAN_LOW_SPEED,
    FAN_MEDIUM_SPEED,
    FAN_HIGH_SPEED,
    COOLING_WATER_VALVE,
    HEATING_WATER_VALVE,
    AUX_HEATING_STAGE1,
    AUX_HEATING_STAGE2,
    FAN_ENABLED,
    OCCUPIED_ENABLED,
    FACE_BYPASS_DAMPER,
    DCV_DAMPER,
    HUMIDIFIER,
    DEHUMIDIFIER,
    EXTERNALLY_MAPPED
}
